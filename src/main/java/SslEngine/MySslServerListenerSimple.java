package SslEngine;

import java.io.IOException;

public class MySslServerListenerSimple implements MySslServerListener{
    public MySslServerListenerSimple(){}
    public void onReadReceived(MySslServerConnection connection) {
        try {
            String message = connection.read(70000);
            if(message!=null) {
                System.out.println("received: "+ message);
                connection.write("hi from server");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
