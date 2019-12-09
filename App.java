import java.io.IOException;

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

import pro.javacard.AID;
import pro.javacard.gp.*;

public class App
{
    // final GPSession gp;

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

        // Establish connection
        try {
            card = reader.connect("*");
            // We use apdu4j which by default uses jnasmartcardio
            // which uses real SCardBeginTransaction
            card.beginExclusive();
            channel = CardChannelBIBO.getBIBO(card.getBasicChannel());
            /*
            System.out.println("Reader: " + reader.getName());
            System.out.println("ATR: " +
            HexUtils.bin2hex(card.getATR().getBytes()));
            System.out.println("More information about your card:");
            System.out.println("    http://smartcard-atr.appspot.com/parse?ATR="
            + HexUtils.bin2hex(card.getATR().getBytes())); System.out.println();
            */
        } catch (CardException e) {
            System.err.println("Could not connect to " + reader.getName() + ": "
                               + TerminalManager.getExceptionMessage(e));
            // continue;
            // break;
            return;
        }

        AID target = AID.fromString("F76964706173730101000101");

        ResponseAPDU r = channel.transmit(new CommandAPDU(
            0x00, ISO7816.INS_SELECT, 0x04, 0x00, target.getBytes()));
        System.out.println(r.getSW());
        prettyOut(r.getData());

        CommandAPDU c = new CommandAPDU(HexUtils.stringToBin("001B0000"));
        r = channel.transmit(c);
        System.out.println(r.getSW());
        prettyOut(r.getData());

        // gp = GPSession.discover(channel);

        /*

        gp = GlobalPlatform.discover(channel);

        // Authenticate to the card
        GPSessionKeyProvider keys =
        PlaintextKeys.fromMasterKey(GPData.getDefaultKey()); EnumSet<APDUMode>
        mode = GlobalPlatform.defaultMode.clone(); gp.openSecureChannel(keys,
        null, 0, mode);

        // Load and Install the cap file
        install(instParams, true);
        channel.close();
        */

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
