package frc.robot;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.EnumSet;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEvent;
import edu.wpi.first.networktables.NetworkTableInstance;

/**
 * Targeting tone example.
 * 
 * Listens for network tables changes on specific keys and plays missile-seeking
 * and missile-lock tones.
 * 
 * To try this out, fire up a networktables server, e.g. glass or outlineviewer,
 * in server mode (use a blank "Listen Address"), and add two booleans:
 * /tone/lock and /tone/search. Run this app ("Simulate Robot" will do it), and
 * twiddle the networktable values. The audio will play accordingly.
 */
public final class Tone {
  private Clip searchClip;
  private Clip lockClip;
  private boolean initialized = false;

  /**
   * Reads the wav files. To avoid checked exceptions here, there's an
   * "initialized" flag.
   */
  public Tone() {
    try {
      // audio files go in src/main/resources, not deploy, this is a desktop app.
      searchClip = AudioSystem.getClip();
      lockClip = AudioSystem.getClip();
      searchClip.open(
          AudioSystem.getAudioInputStream(
              new BufferedInputStream(getClass().getResourceAsStream("/search.wav"))));
      lockClip
          .open(AudioSystem.getAudioInputStream(
              new BufferedInputStream(getClass().getResourceAsStream("/lock.wav"))));
      initialized = true;
    } catch (IOException | LineUnavailableException | UnsupportedAudioFileException e) {
      e.printStackTrace();
    }
  }

  /**
   * Loops forever, listening for network tables changes and playing sounds.
   */
  public void run() {
    if (!initialized) {
      System.out.println("initialization failed, exiting");
      return;
    }
    System.out.println("running");

    NetworkTableInstance inst = NetworkTableInstance.getDefault();
    inst.startClient4("Tone App");
    inst.setServer("localhost");
    NetworkTable table = inst.getTable("tone");

    // You need to set initial values; the glass instance can't create them.
    table.getEntry("search").setBoolean(false);
    table.getEntry("lock").setBoolean(false);

    inst.addListener(
        table.getEntry("search"),
        EnumSet.of(NetworkTableEvent.Kind.kValueAll),
        (event) -> play(event, searchClip));
    inst.addListener(
        table.getEntry("lock"),
        EnumSet.of(NetworkTableEvent.Kind.kValueAll),
        (event) -> play(event, lockClip));



    System.out.println("entering loop");
    while (true) {
      try {
        if (inst.isConnected()) {
          System.out.println("connected");
        } else {
          System.out.println("not connected");
        }
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  /** Plays a clip or stops it, depending on the event. */
  private static void play(NetworkTableEvent event, Clip clip) {
    if (event.valueData.value.getBoolean()) {
      System.out.println(event.valueData.getTopic().getName() + " on");
      clip.loop(Clip.LOOP_CONTINUOUSLY);
    } else {
      System.out.println(event.valueData.getTopic().getName() + " off");
      clip.stop();
    }
  }

  public static void main(String... args) {
    new Tone().run();
  }
}
