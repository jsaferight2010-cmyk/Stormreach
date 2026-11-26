package com.stormreach.tornado;

import com.stormreach.Stormreach;
import com.stormreach.sim.EFScale;
import com.stormreach.sim.WindField;
import com.stormreach.weather.cloud.StormCellEntity;
import com.stormreach.weather.precip.RainAccumulation;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The weather screens. Every instrument opens a screen made for it (an anemometer shows the wind, a thermometer the
 * air, the Doppler radar a live radar scope...), and the Weather Monitor is the central interface that shows
 * everything, including the radar when a station and a radar are linked to it. The server measures; the client draws.
 * While a screen is open the client asks for a fresh reading once a second; the server checks the player is still
 * at the instrument (or holding the device) before answering.
 */
public final class WeatherUi {
    private WeatherUi() {}

    /** Which device a screen is for. The ordinals of InstrumentBlock.Kind come first, then handheld devices. */
    public static final int STORM_RADAR_ITEM = 100, WEATHER_RADIO_ITEM = 101;

    public record Contact(float dx, float dz, float distance, float coupletMph, float motionMph, float headingDeg, int estRating,
                          float width, int morph, boolean multiVortex, int formation) {}

    public record Cell(float dx, float dz, float growth, float width, boolean severe) {}

    public record Core(float dx, float dz, float radius, int kind) {} // kind 0 hail, 1 wet downburst, 2 dry downburst

    public record Report(int device, BlockPos pos, float windAvg, float windGust, String windFrom, float tempC, float humidity, float dewC,
                         float pressure, int sky, float wetness, String forecast, String risk, float range, boolean linked,
                         List<Contact> contacts, List<Cell> cells, List<Core> cores, RadarScan.Grid scan) {}

    private static void writeStr(FriendlyByteBuf b, String s) { b.writeUtf(s, 512); }

    static void write(FriendlyByteBuf b, Report r) {
        b.writeVarInt(r.device()); b.writeBlockPos(r.pos());
        b.writeFloat(r.windAvg()); b.writeFloat(r.windGust()); writeStr(b, r.windFrom());
        b.writeFloat(r.tempC()); b.writeFloat(r.humidity()); b.writeFloat(r.dewC()); b.writeFloat(r.pressure());
        b.writeVarInt(r.sky()); b.writeFloat(r.wetness()); writeStr(b, r.forecast()); writeStr(b, r.risk()); b.writeFloat(r.range()); b.writeBoolean(r.linked());
        b.writeVarInt(r.contacts().size());
        for (Contact c : r.contacts()) {
            b.writeFloat(c.dx()); b.writeFloat(c.dz()); b.writeFloat(c.distance()); b.writeFloat(c.coupletMph()); b.writeFloat(c.motionMph());
            b.writeFloat(c.headingDeg()); b.writeVarInt(c.estRating() + 1); b.writeFloat(c.width()); b.writeVarInt(c.morph()); b.writeBoolean(c.multiVortex()); b.writeVarInt(c.formation());
        }
        b.writeVarInt(r.cells().size());
        for (Cell c : r.cells()) { b.writeFloat(c.dx()); b.writeFloat(c.dz()); b.writeFloat(c.growth()); b.writeFloat(c.width()); b.writeBoolean(c.severe()); }
        b.writeVarInt(r.cores().size());
        for (Core c : r.cores()) { b.writeFloat(c.dx()); b.writeFloat(c.dz()); b.writeFloat(c.radius()); b.writeVarInt(c.kind()); }
        b.writeBoolean(r.scan() != null);
        if (r.scan() != null) { b.writeByteArray(r.scan().dbz()); b.writeByteArray(r.scan().velocity()); }
    }

    static Report read(FriendlyByteBuf b) {
        int device = b.readVarInt(); BlockPos pos = b.readBlockPos();
        float wa = b.readFloat(), wg = b.readFloat(); String from = b.readUtf(512);
        float t = b.readFloat(), rh = b.readFloat(), dew = b.readFloat(), p = b.readFloat();
        int sky = b.readVarInt(); float wet = b.readFloat(); String fc = b.readUtf(512), risk = b.readUtf(512); float range = b.readFloat(); boolean linked = b.readBoolean();
        int n = Math.min(64, b.readVarInt());
        List<Contact> contacts = new ArrayList<>();
        for (int i = 0; i < n; i++) contacts.add(new Contact(b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(),
                b.readVarInt() - 1, b.readFloat(), b.readVarInt(), b.readBoolean(), b.readVarInt()));
        n = Math.min(64, b.readVarInt());
        List<Cell> cells = new ArrayList<>();
        for (int i = 0; i < n; i++) cells.add(new Cell(b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readBoolean()));
        n = Math.min(64, b.readVarInt());
        List<Core> cores = new ArrayList<>();
        for (int i = 0; i < n; i++) cores.add(new Core(b.readFloat(), b.readFloat(), b.readFloat(), b.readVarInt()));
        RadarScan.Grid scan = null;
        if (b.readBoolean()) {
            int n2 = com.stormreach.sim.RadarMath.AZIMUTHS * com.stormreach.sim.RadarMath.GATES;
            byte[] dbz = b.readByteArray(n2), vel = b.readByteArray(n2);
            if (dbz.length == n2 && vel.length == n2) scan = new RadarScan.Grid(dbz, vel);
        }
        return new Report(device, pos, wa, wg, from, t, rh, dew, p, sky, wet, fc, risk, range, linked, contacts, cells, cores, scan);
    }

    /** Server to client: open (or refresh) the weather screen with this reading. */
    public record ReportPayload(boolean open, Report report) implements CustomPacketPayload {
        public static final Type<ReportPayload> TYPE = new Type<>(Stormreach.id("weather_report"));
        public static final StreamCodec<FriendlyByteBuf, ReportPayload> CODEC = StreamCodec.ofMember(
                (p, b) -> { b.writeBoolean(p.open()); write(b, p.report()); }, b -> new ReportPayload(b.readBoolean(), read(b)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client to server: a fresh reading, please. */
    public record RefreshPayload(int device, BlockPos pos) implements CustomPacketPayload {
        public static final Type<RefreshPayload> TYPE = new Type<>(Stormreach.id("weather_refresh"));
        public static final StreamCodec<FriendlyByteBuf, RefreshPayload> CODEC = StreamCodec.ofMember(
                (p, b) -> { b.writeVarInt(p.device()); b.writeBlockPos(p.pos()); }, b -> new RefreshPayload(b.readVarInt(), b.readBlockPos()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void init() {
        PayloadTypeRegistry.clientboundPlay().register(ReportPayload.TYPE, ReportPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RefreshPayload.TYPE, RefreshPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(RefreshPayload.TYPE, (payload, ctx) -> ctx.server().execute(() -> {
            ServerPlayer p = ctx.player();
            boolean handheld = payload.device() >= STORM_RADAR_ITEM;
            if (stillAt(p, payload.device(), payload.pos())) send(p, payload.device(), handheld ? p.blockPosition() : payload.pos(), false);
        }));
    }

    static boolean stillAt(ServerPlayer p, int device, BlockPos pos) {
        if (device == STORM_RADAR_ITEM) return p.isHolding(WeatherItems.STORM_RADAR);
        if (device == WEATHER_RADIO_ITEM) return p.isHolding(WeatherItems.WEATHER_RADIO);
        return p.level().isLoaded(pos) && p.distanceToSqr(Vec3.atCenterOf(pos)) < 10 * 10
                && (p.level().getBlockState(pos).getBlock() instanceof InstrumentBlock
                    || p.level().getBlockState(pos).getBlock() instanceof com.stormreach.block.StormBarometerBlock
                    || p.level().getBlockState(pos).getBlock() instanceof com.stormreach.block.WindVaneBlock);
    }

    /** Opens the device's screen for the player. */
    public static void open(ServerPlayer p, int device, BlockPos pos) {
        send(p, device, pos, true);
        if (device == InstrumentBlock.Kind.TORNADO_RADAR.ordinal() || device == STORM_RADAR_ITEM)
            p.level().playSound(null, pos, com.stormreach.registry.ModSfx.BLOCK_RADAR_PING, net.minecraft.sounds.SoundSource.BLOCKS, 0.6f, 1.0f);
    }

    static void send(ServerPlayer p, int device, BlockPos pos, boolean open) {
        if (!ServerPlayNetworking.canSend(p, ReportPayload.TYPE)) return;
        ServerPlayNetworking.send(p, new ReportPayload(open, measure((ServerLevel) p.level(), device, pos)));
    }

    /** Takes a full reading at {@code pos} for {@code device}. Public for GameTests. */
    public static Report measure(ServerLevel level, int device, BlockPos pos) {
        double[] wind = Instruments.windMph(level, pos);
        double t = Instruments.temperatureC(level, pos), rh = Instruments.humidity(level, pos);
        boolean central = device == InstrumentBlock.Kind.WEATHER_MONITOR.ordinal();
        boolean linked = !central || InstrumentBlock.monitorLinked(level, pos);
        boolean radar = device == InstrumentBlock.Kind.TORNADO_RADAR.ordinal() || device == STORM_RADAR_ITEM || (central && linked);
        float range = device == STORM_RADAR_ITEM ? 256 : device == InstrumentBlock.Kind.TORNADO_RADAR.ordinal() ? 1024 : central ? 2048 : 512;
        Vec3 c = Vec3.atCenterOf(pos);
        List<Contact> contacts = new ArrayList<>();
        List<Cell> cells = new ArrayList<>();
        List<Core> cores = new ArrayList<>();
        boolean detector = device == InstrumentBlock.Kind.STORM_DETECTOR.ordinal() || device == WEATHER_RADIO_ITEM;
        if (radar || detector) {
            for (TornadoEntity tor : TornadoManager.live(level)) {
                double d = tor.position().distanceTo(c);
                if (d > range) continue;
                WindField f = tor.windField();
                EFScale est = tor.estimatedRating();
                contacts.add(new Contact((float) (tor.getX() - c.x), (float) (tor.getZ() - c.z), (float) d, (float) EFScale.msToMph(f.vMax * 2),
                        (float) EFScale.msToMph(Math.hypot(f.translateX, f.translateZ)), (float) Math.toDegrees(Math.atan2(f.translateX, -f.translateZ)),
                        est == null ? -1 : est.ordinal(), tor.visualWidth(), tor.morphology().ordinal(), tor.subvortices() > 0, tor.formation().ordinal()));
                if (contacts.size() >= 32) break;
            }
        }
        if (radar) {
            for (StormCellEntity cell : level.getEntitiesOfClass(StormCellEntity.class, new AABB(pos).inflate(range, 400, range))) {
                cells.add(new Cell((float) (cell.getX() - c.x), (float) (cell.getZ() - c.z), cell.growth(), cell.cloudWidth(), cell.severe()));
                if (cells.size() >= 32) break;
            }
            for (WeatherEvents.ActiveEvent e : TornadoManager.events()) {
                if (e.level != level || e.centre.distanceTo(c) > range) continue;
                if (e instanceof WeatherEvents.HailBurst h) cores.add(new Core((float) (h.centre.x - c.x), (float) (h.centre.z - c.z), (float) h.radius, 0));
                else if (e instanceof WeatherEvents.Downburst db) cores.add(new Core((float) (db.centre.x - c.x), (float) (db.centre.z - c.z), (float) db.radius, db.wet ? 1 : 2));
                if (cores.size() >= 32) break;
            }
        }
        RadarScan.Grid scan = null;
        if (radar) {
            List<TornadoEntity> tors = new ArrayList<>();
            for (TornadoEntity tor : TornadoManager.live(level)) if (tor.position().distanceTo(c) < range * 1.2) tors.add(tor);
            List<StormCellEntity> sc = level.getEntitiesOfClass(StormCellEntity.class, new AABB(pos).inflate(range * 1.1, 400, range * 1.1));
            List<WeatherEvents.ActiveEvent> ev = new ArrayList<>();
            for (WeatherEvents.ActiveEvent e : TornadoManager.events()) if (e.level == level && e.centre.distanceTo(c) < range * 1.2) ev.add(e);
            scan = RadarScan.scan(level, c, range, tors, sc, ev);
        }
        int sky = level.isThundering() ? 2 : level.isRaining() ? 1 : 0;
        String from = com.stormreach.wind.WindSources.fromAt(level, Vec3.atCenterOf(pos));   // 6.0: a Wind Generator's wind wins locally
        return new Report(device, pos, (float) wind[0], (float) wind[1], from != null ? from : Instruments.windFrom(), (float) t, (float) rh, (float) Instruments.dewPointC(t, rh),
                (float) Instruments.pressureHpa(level, pos), sky, (float) RainAccumulation.wetness(level), Instruments.forecast(level),
                Instruments.stormRisk(level, pos), range, linked, contacts, cells, cores, scan);
    }
}
