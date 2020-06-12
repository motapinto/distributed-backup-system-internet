package Chord.Message;


import Chord.ChordID;

import java.net.InetSocketAddress;

public class ChordMessageParser {
    public static InetSocketAddress parseAddress(String address) {
        String ipPort = address.split("/")[1];
        return new InetSocketAddress(ipPort.split(":")[0], Integer.parseInt(ipPort.split(":")[1]));
    }

    public static ChordMessage parseMessage(String message) {
        if (message == null)
            return new NullChordMessage("NULL");
        try {
            String[] tokens = message.split(" ");
            String type = tokens[0], addressStr = tokens[1];
            InetSocketAddress senderAddress = parseAddress(addressStr);
            switch (type) {
                case "GETPRED": return new GetPredecessorMessage(senderAddress);
                case "GETSUCC": return new GetSuccessorMessage(senderAddress);
                case "SETPRED": return new SetPredecessorMessage(parseAddress(tokens[2]), senderAddress);
                case "LOOKUP": return new LookupMessage(ChordID.fromDec(tokens[2]), senderAddress);
                default: return new NullChordMessage(message);
            }
        }
        catch (Exception e) {
            return new NullChordMessage(message);
        }
    }
}
