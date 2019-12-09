import java.io.IOException;
import java.util.EnumSet;

import javax.smartcardio.TerminalFactory;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;

import apdu4j.APDUBIBO;
import apdu4j.CardChannelBIBO;
import apdu4j.TerminalManager;
import apdu4j.HexUtils;
import apdu4j.CommandAPDU;
import apdu4j.ResponseAPDU;

import pro.javacard.gp.GPSession.APDUMode;
import pro.javacard.gp.GPSession.GPSpec;
import pro.javacard.AID;
import pro.javacard.gp.*;

public class App
{
    public static void main(String args[]) throws CardException, IOException
    {
        System.out.println("---begin---");
        TerminalFactory tf = TerminalFactory.getDefault();
        CardTerminal reader = null;

        try {
            for (CardTerminal t : tf.terminals().list()) {
                if (t.isCardPresent()) {
                    reader = t;
                    break;
                }
            }
        } catch (CardException e) {
            System.out.println("Error listing card terminals");
        }

        if (reader == null) {
            System.out.println("No available PC/SC terminal");
            return;
        }

        Card card;
        APDUBIBO channel = null;

        try {
            card = reader.connect("*");
            card.beginExclusive();
            channel = CardChannelBIBO.getBIBO(card.getBasicChannel());
        } catch (CardException e) {
            System.err.println("Could not connect to " + reader.getName() + ": "
                               + TerminalManager.getExceptionMessage(e));
            return;
        }

        AID authaid = AID.fromString("F76964706173730101000101");
        AID cmaid = AID.fromString("A000000151000000");

        // 1) select AUTH applet
        ResponseAPDU r = channel.transmit(new CommandAPDU(
            0x00, ISO7816.INS_SELECT, 0x04, 0x00, authaid.getBytes()));
        System.out.println(String.format("0x%08X", r.getSW()));
        prettyOut(r.getData());

        // 2) open secure channel
        final GPSession gp = GPSession.discover(channel);

        PlaintextKeys keyz;
        keyz = PlaintextKeys.defaultKey();

        final GPCardKeys keys = keyz;

        EnumSet<APDUMode> mode = GPSession.defaultMode.clone();
        mode.clear();
        mode.add(APDUMode.fromString("mac"));
        mode.add(APDUMode.fromString("enc"));
        gp.openSecureChannel(keys, null, null, mode);

        System.out.println("++++++++++++++++++++++++++++++++++");
        // 3) Query security level
        CommandAPDU cc = new CommandAPDU(HexUtils.stringToBin("001B0000"));
        r = gp.transmit(cc);
        System.out.println(String.format("0x%08X", r.getSW()));
        prettyOut(r.getData());

        System.out.println("--- end ---");
    }

    public static void prettyOut(byte[] msg)
    {
        for (int j = 1; j < msg.length + 1; j++) {
            if (j % 8 == 1 || j == 0) {
                if (j != 0) {
                    System.out.println();
                }
                System.out.format("0%d\t|\t", j / 8);
            }
            System.out.format("%02X", msg[j - 1]);
            if (j % 4 == 0) {
                System.out.print(" ");
            }
        }
        System.out.println();
    }
}
