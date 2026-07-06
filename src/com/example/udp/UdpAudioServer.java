package com.example.udp;

import javax.sound.sampled.*;
import java.net.*;
import java.util.Arrays;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UdpAudioServer {
    public static void main(String[] args) throws Exception {
        int port = 55555;
        String group = null;
        if (args.length > 0) port = Integer.parseInt(args[0]);
        if (args.length > 1) group = args[1];

        final byte[] buf = new byte[65507];
        SourceDataLine line = null;
        AudioFormat currentFormat = null;
        ByteArrayOutputStream capture = null;
        boolean dumpEnabled = false;
        String dumpPath = null;

        boolean useNativePlayer = false;

        DatagramSocket socket = null;
        MulticastSocket mcast = null;
        InetAddress groupAddr = null;

        boolean playAfterDump = false;
        if (args.length > 2) {
            dumpEnabled = true;
            dumpPath = args[2];
        }
        if (args.length > 3 && ("play".equalsIgnoreCase(args[3]) || "yes".equalsIgnoreCase(args[3]) )) {
            playAfterDump = true;
        }
        if (args.length > 4 && "native".equalsIgnoreCase(args[4])) {
            useNativePlayer = true;
        }

        if (group != null && InetAddress.getByName(group).isMulticastAddress()) {
            groupAddr = InetAddress.getByName(group);
            mcast = new MulticastSocket(port);
            mcast.joinGroup(groupAddr);
            socket = mcast;
            System.out.println("Serveur UDP audio (multicast) joint le groupe " + group + " sur le port " + port);
        } else {
            socket = new DatagramSocket(port);
            System.out.println("Serveur UDP audio démarré sur le port " + port);
        }

        while (true) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);
            int len = packet.getLength();
            if (len == 0) continue;

            byte[] data = Arrays.copyOf(packet.getData(), len);

            String ascii = null;
            try {
                ascii = new String(data, "UTF-8");
            } catch (Exception ignored) {}

            if (ascii != null && ascii.startsWith("FMT:")) {
                String[] parts = ascii.trim().split(":");
                if (parts.length >= 6) {
                    float sampleRate = Float.parseFloat(parts[1]);
                    int sampleSize = Integer.parseInt(parts[2]);
                    int channels = Integer.parseInt(parts[3]);
                    boolean signed = Boolean.parseBoolean(parts[4]);
                    boolean bigEndian = Boolean.parseBoolean(parts[5]);

                    currentFormat = new AudioFormat(sampleRate, sampleSize, channels, signed, bigEndian);
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, currentFormat);
                    if (!AudioSystem.isLineSupported(info)) {
                        System.err.println("Format audio non supporté: " + currentFormat);
                        continue;
                    }
                    if (line != null) {
                        line.drain();
                        line.stop();
                        line.close();
                    }
                    line = (SourceDataLine) AudioSystem.getLine(info);
                    line.open(currentFormat);
                    line.start();
                    System.out.println("Format reçu et ligne ouverte: " + currentFormat);
                    // préparer capture si demandé
                    if (dumpEnabled) {
                        capture = new ByteArrayOutputStream();
                        System.out.println("Dump activé — capture en mémoire démarrée");
                    }
                }
                continue;
            }

            if (ascii != null && ascii.equals("END")) {
                System.out.println("Réception: END — fin de la transmission actuelle");
                if (line != null) {
                    line.drain();
                    line.stop();
                    line.close();
                    line = null;
                }
                if (dumpEnabled && capture != null) {
                    try {
                        byte[] audioBytes = capture.toByteArray();
                        if (audioBytes.length > 0) {
                            // écrire WAV
                            String ts = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
                            String outName = dumpPath == null || dumpPath.isEmpty() ? "dump-" + ts + ".wav" : (dumpPath.endsWith("/") ? dumpPath + "dump-" + ts + ".wav" : dumpPath + "-" + ts + ".wav");
                            File outFile = new File(outName);
                            if (currentFormat != null) {
                                ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
                                AudioInputStream ais = new AudioInputStream(bais, currentFormat, audioBytes.length / currentFormat.getFrameSize());
                                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, outFile);
                                ais.close();
                                bais.close();
                                System.out.println("Dump écrit : " + outFile.getAbsolutePath());
                                if (playAfterDump) {
                                    try {
                                        // tenter de jouer le fichier WAV
                                        AudioInputStream playStream = AudioSystem.getAudioInputStream(outFile);
                                        DataLine.Info infoLine = new DataLine.Info(Clip.class, playStream.getFormat());
                                        if (AudioSystem.isLineSupported(infoLine)) {
                                            Clip clip = (Clip) AudioSystem.getLine(infoLine);
                                            clip.open(playStream);
                                            clip.start();
                                            System.out.println("Lecture du dump en cours...");
                                            // ne pas bloquer indéfiniment : attendre la fin du clip
                                            while (clip.isRunning()) Thread.sleep(100);
                                            clip.close();
                                            playStream.close();
                                            System.out.println("Lecture terminée.");
                                        } else {
                                            System.err.println("Lecture impossible : format non supporté par la ligne native");
                                        }
                                    } catch (Exception e) {
                                        System.err.println("Erreur lors de la lecture du dump : " + e.getMessage());
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Erreur lors de l'écriture du dump : " + e.getMessage());
                    } finally {
                        capture = null;
                    }
                }
                continue;
            }

            if (line == null) {
                System.err.println("Données reçues avant le format — ignorées");
                continue;
            }
            line.write(data, 0, data.length);
            if (dumpEnabled && capture != null) {
                try { capture.write(data); } catch (IOException ignored) {}
            }
        }
    }
}
