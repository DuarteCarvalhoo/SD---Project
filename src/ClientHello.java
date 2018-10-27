import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientHello extends Remote {
    void msg(String s) throws RemoteException;
}
