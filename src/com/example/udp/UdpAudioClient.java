package com.example.udp;

import javax.sound.sampled.*;
import java.io.*;
import java.net.*;

public class UdpAudioClient {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java com.example.udp.UdpAudioClient <host> [port] [wav-file]");
            System.out.println("Si le fichier wav n'est pas fourni, un ton test sera généré.");
            return;
        }

        String host = args[0];
        int port = 55555;
        if (args.length >= 2) port = Integer.parseInt(args[1]);

        InetAddress address = InetAddress.getByName(host);
        DatagramSocket socket = new DatagramSocket();

        AudioFormat format;
        InputStream audioStream;

        if (args.length >= 3) {
            File wav = new File(args[2]);
            AudioInputStream ais = AudioSystem.getAudioInputStream(wav);
            AudioFormat base = ais.getFormat();
            AudioFormat target = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                base.getSampleRate(), 16, base.getChannels(), base.getChannels() * 2,
                base.getSampleRate(), false);
            AudioInputStream din = AudioSystem.isConversionSupported(target, base) ?
                AudioSystem.getAudioInputStream(target, ais) : AudioSystem.getAudioInputStream(target, ais);
            format = din.getFormat();
            audioStream = din;
            System.out.println("Lecture du fichier: " + wav.getName() + " -> format: " + format);
        } else {
            // Générer un ton 440Hz mono 3s, 44100Hz, 16 bits
            float sampleRate = 44100f;
            format = new AudioFormat(sampleRate, 16, 1, true, false);
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            int durationSec = 3;
            double freq = 440.0;
            int frames = (int) (durationSec * sampleRate);
            for (int i = 0; i < frames; i++) {
                double angle = 2.0 * Math.PI * i * freq / sampleRate;
                short s = (short) (Math.sin(angle) * Short.MAX_VALUE * 0.5);
                bout.write(s & 0xff);
                bout.write((s >> 8) & 0xff);
            }
            audioStream = new ByteArrayInputStream(bout.toByteArray());
            System.out.println("Génération d'un ton de test (3s, 440Hz)");
        }

        // Envoyer l'entête de format
        String fmt = String.format("FMT:%.0f:%d:%d:%b:%b",
            format.getSampleRate(), format.getSampleSizeInBits(), format.getChannels(),
            format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED), format.isBigEndian());
        byte[] fmtb = fmt.getBytes("UTF-8");
        socket.send(new DatagramPacket(fmtb, fmtb.length, address, port));

        // Envoyer les données en petits paquets UDP
        byte[] buf = new byte[1024];
        int read;
        while ((read = audioStream.read(buf)) != -1) {
            socket.send(new DatagramPacket(buf, read, address, port));
            Thread.sleep(2); // petit délai pour lisser la réception
        }

        // Envoyer END
        byte[] end = new byte[] { 'E', 'N', 'D' };
        socket.send(new DatagramPacket(end, end.length, address, port));
        System.out.println("Transmission terminée.");

        socket.close();
    }
}
