package ca.keal.varianttap.gpgs;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * A helper class for implementing a {@link ServiceConnection} to {@link GPGSHelperService}. Classes
 * using {@link GPGSHelperService} must pass a {@link GPGSHelperClient} to this class'
 * constructor and must store the service passed to
 * {@link GPGSHelperClient#receiveService(GPGSHelperService)};
 */
public class GPGSHelperServiceConnection implements ServiceConnection {
  
  private static final String TAG = "GPGSHelperServiceConn"; // must be <23 chars
  
  private final GPGSHelperClient receiver;
  
  public GPGSHelperServiceConnection(GPGSHelperClient receiver) {
    this.receiver = receiver;
  }
  
  @Override
  public void onServiceConnected(ComponentName name, IBinder binder) {
    GPGSHelperService.GPGSHelperBinder helperBinder = (GPGSHelperService.GPGSHelperBinder) binder;
    GPGSHelperService service = helperBinder.getService();
    receiver.receiveService(service);
  }
  
  @Override
  public void onServiceDisconnected(ComponentName name) {
    Log.e(TAG, "GPGSHelperService disconnected! This should not happen!");
  }
  
}