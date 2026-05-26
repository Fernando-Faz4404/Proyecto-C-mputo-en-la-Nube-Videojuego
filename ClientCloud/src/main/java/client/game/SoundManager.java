package client.game;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SoundManager {

    private static final SoundManager INSTANCE = new SoundManager();
    public static SoundManager get() { return INSTANCE; }

    private final Map<String, Clip> clips = new HashMap<>();
    private volatile Thread bgmThread;
    private volatile boolean bgmRunning;
    private volatile SourceDataLine bgmLine;
    private volatile String currentBgmPath;
    private volatile Thread menuFadeThread;
    private boolean loaded;

    private SoundManager() {}

    public void loadAll() {
        if (loaded) return;
        loaded = true;
        loadClip("shoot","/sonidosTank/mixkit-fast-rocket-whoosh-1714.wav");
        loadClip("hit_wall","/sonidosTank/explosion8bits.wav");
        loadClip("hit_tank","/sonidosTank/tankdañado.wav");
        loadClip("damaged","/sonidosTank/alarmadedañodetank.wav");
        loadClip("death","/sonidosTank/muerte8bits.wav");
        loadClip("explosion","/sonidosTank/explosionmasrealista.wav");
        loadClip("powerup_speed","/sonidosTank/velocidadtank.wav");
        loadClip("powerup_ammo","/sonidosTank/itemmunicion.wav");
        loadClip("powerup_immunity","/sonidosTank/alaramdetank2.wav");
        loadClip("powerup_health","/sonidosTank/aistankvida.mp3");
        loadClip("engine","/sonidosTank/motortank2.mp3");
        loadClip("menu_bgm","/sonidosTank/musicadefondo.mp3");
    }

    private void loadClip(String key, String resource) {
        try {
            InputStream is = getClass().getResourceAsStream(resource);
            if (is == null) { System.err.println("[Sound] Missing: " + resource); return; }
            AudioInputStream raw = AudioSystem.getAudioInputStream(new BufferedInputStream(is));
            AudioInputStream pcm = toPcm(raw);
            Clip clip = AudioSystem.getClip();
            clip.open(pcm);
            clips.put(key, clip);
        } catch (Exception e) {
            System.err.println("[Sound] Load failed " + resource + ": " + e.getMessage());
        }
    }

    private AudioInputStream toPcm(AudioInputStream src) throws Exception {
        AudioFormat fmt = src.getFormat();
        if (fmt.getEncoding() == AudioFormat.Encoding.PCM_SIGNED) return src;
        AudioFormat pcmFmt = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                fmt.getSampleRate(), 16,
                fmt.getChannels(), fmt.getChannels() * 2,
                fmt.getSampleRate(), false);
        return AudioSystem.getAudioInputStream(pcmFmt, src);
    }

    public void play(String key) {
        Clip c = clips.get(key);
        if (c == null) return;
        c.stop();
        c.setFramePosition(0);
        c.start();
    }

    public void setEngine(boolean on) {
        Clip c = clips.get("engine");
        if (c == null) return;
        if (on) {
            if (!c.isRunning()) {
                c.setFramePosition(0);
                c.loop(Clip.LOOP_CONTINUOUSLY);
            }
        } else {
            if (c.isRunning()) c.stop();
        }
    }

    public void playMenuMusic() {
        stopBgm();
        Clip c = clips.get("menu_bgm");
        if (c == null || c.isRunning()) return;
        setClipVolume(c, -80f);
        c.setFramePosition(0);
        c.loop(Clip.LOOP_CONTINUOUSLY);
        Thread t = new Thread(() -> menuFadeLoop(c), "MenuFade");
        t.setDaemon(true);
        menuFadeThread = t;
        t.start();
    }

    private void menuFadeLoop(Clip c) {
        long total = c.getFrameLength();
        long fade = (long)(c.getFormat().getFrameRate() * 2.0);
        while (c.isRunning() && !Thread.currentThread().isInterrupted()) {
            long pos = c.getLongFramePosition() % total;
            float linear;
            if (pos < fade) {
                linear = (float) pos / fade;
            } else if (pos > total - fade) {
                linear = (float)(total - pos) / fade;
            } else {
                linear = 1f;
            }
            setClipVolume(c, linearToDb(linear));
            try { Thread.sleep(20); } catch (InterruptedException e) { break; }
        }
    }

    private void stopMenuMusic() {
        Thread t = menuFadeThread;
        menuFadeThread = null;
        if (t != null) t.interrupt();
        Clip c = clips.get("menu_bgm");
        if (c != null && c.isRunning()) c.stop();
    }

    private void setClipVolume(Clip c, float gainDb) {
        try {
            FloatControl fc = (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
            fc.setValue(Math.max(fc.getMinimum(), Math.min(0f, gainDb)));
        } catch (Exception ignored) {}
    }

    private float linearToDb(float v) {
        if (v <= 0f) return -80f;
        return (float)(20.0 * Math.log10(v));
    }

    public void playBgm(String mapResource) {
        stopMenuMusic();
        String path = bgmPath(mapResource);
        if (path.equals(currentBgmPath)) return;
        stopBgm();
        currentBgmPath = path;
        bgmRunning = true;
        bgmThread = new Thread(() -> bgmLoop(path), "BGM");
        bgmThread.setDaemon(true);
        bgmThread.start();
    }

    public void stopBgm() {
        bgmRunning = false;
        SourceDataLine l = bgmLine;
        if (l != null) { l.stop(); l.flush(); l.close(); bgmLine = null; }
        Thread t = bgmThread;
        if (t != null) { t.interrupt(); bgmThread = null; }
        currentBgmPath = null;
    }

    public void stopAll() {
        setEngine(false);
        stopMenuMusic();
        stopBgm();
    }

    private void bgmLoop(String path) {
        while (bgmRunning && !Thread.currentThread().isInterrupted()) {
            SourceDataLine line = null;
            try {
                InputStream is = getClass().getResourceAsStream(path);
                if (is == null) return;
                AudioInputStream raw = AudioSystem.getAudioInputStream(new BufferedInputStream(is));
                AudioInputStream pcm = toPcm(raw);
                AudioFormat fmt = pcm.getFormat();
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(fmt, 8192);
                bgmLine = line;
                line.start();
                byte[] buf = new byte[4096];
                int n;
                while (bgmRunning && (n = pcm.read(buf)) != -1) {
                    line.write(buf, 0, n);
                }
                line.drain();
                line.close();
                bgmLine = null;
                pcm.close();
            } catch (Exception e) {
                if (line != null) { try { line.close(); } catch (Exception ignored) {} }
                bgmLine = null;
                if (Thread.currentThread().isInterrupted()) break;
            }
        }
    }

    private String bgmPath(String mapResource) {
        if (mapResource == null) return "/sonidosTank/guerrafondo2.mp3";
        return switch (mapResource) {
            case "/maps/mapaHielo.txt"     -> "/sonidosTank/winterfondo.mp3";
            case "/maps/mapaVolcanico.txt" -> "/sonidosTank/guerrafondo3.mp3";
            default                        -> "/sonidosTank/guerrafondo2.mp3";
        };
    }
}
