import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientHello extends Remote {
    String notificationEditor() throws RemoteException;
}
