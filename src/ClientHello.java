import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientHello extends Remote {
    String msg(String s) throws RemoteException;
}
