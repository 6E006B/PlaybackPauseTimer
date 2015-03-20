package info.n00k.playbackpausetimer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class PPTService extends Service {

    public static final String PPT_PAUSE_DELAY_ID = "info.n00k.playbackpausetimer.m_delay";
    public static final int FOREGROUND_NOTIFICATION_ID = 1;

    private int m_delay;
    private Timer m_timer;
    private long m_target_time;
    private final IBinder m_binder = new PPTBinder();
    private CountDownTimer m_count_down_timer;
    NotificationCompat.Builder m_notification_builder;

    public PPTService() {
        m_notification_builder = new NotificationCompat.Builder(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(BuildConfig.DEBUG) {
            Toast.makeText(this, "PPT service starting", Toast.LENGTH_SHORT).show();
        }
        Log.d("PPT", "service starting");
        handleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        if(BuildConfig.DEBUG) {
            Toast.makeText(this, "PPT binding service", Toast.LENGTH_SHORT).show();
        }
        Log.d("PPT", "binding service");
        handleIntent(intent);
        return m_binder;
    }

    @Override
    public void onDestroy() {
        if(m_timer != null) {
            m_timer.cancel();
            m_timer = null;
        }
        if(BuildConfig.DEBUG) {
            Toast.makeText(this, "PPT service done", Toast.LENGTH_SHORT).show();
        }
        Log.d("PPT", "service done");
    }

    private void handleIntent(Intent intent) {
        if(intent.hasExtra(PPT_PAUSE_DELAY_ID)) {
            m_delay = intent.getIntExtra(PPT_PAUSE_DELAY_ID, 0);
            if(BuildConfig.DEBUG) {
                Toast.makeText(this, "PPT delay set to " + Integer.toString(m_delay), Toast.LENGTH_SHORT).show();
            }
            Log.d("PPT", "delay set to " + Integer.toString(m_delay));
            resetTimer();
        } else {
            Log.d("PPT", "handling intent without delay");
        }
    }

    private void resetTimer() {
        if(m_timer != null) {
            m_timer.cancel();
            m_target_time = 0;
            if(BuildConfig.DEBUG) {
                Toast.makeText(this, "PPT removed previous timer", Toast.LENGTH_SHORT).show();
            }
            Log.d("PPT",  "removed previous timer");
            stopChronometer();
        }
        if(m_delay > 0) {
            m_target_time = (new Date()).getTime() + (60000 * m_delay);
            Log.d("PPT", "pause at " + (new Date(m_target_time)).toString());
            m_timer = new Timer();
            m_timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Intent intent = new Intent("com.android.music.musicservicecommand");
                    intent.putExtra("command", "pause");
                    sendBroadcast(intent);
                    Log.d("PPT", "EXECUTE!!!!11!1");
                    m_timer = null;
                    PPTService.this.stopSelf();
                }
            }, m_delay * 60000);
            if(BuildConfig.DEBUG) {
                Toast.makeText(this, "PPT reset timer to " + Integer.toString(m_delay), Toast.LENGTH_SHORT).show();
            }
            Log.d("PPT", "reset timer to " + Integer.toString(m_delay));
            setForeground();
            startChronometer();
        } else {
            if(BuildConfig.DEBUG) {
                Toast.makeText(this, "PPT disabled timer", Toast.LENGTH_SHORT).show();
            }
            Log.d("PPT", "disabled timer");
            m_timer = null;
            PPTService.this.stopSelf();
        }
    }

    public void startChronometer() {
        if(m_count_down_timer != null) {
            m_count_down_timer.cancel();
        }
        long remaining_time = m_target_time - (new Date()).getTime();
        m_count_down_timer = new CountDownTimer(remaining_time, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                setNotificationTimer(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                stopChronometer();
            }
        };
        m_count_down_timer.start();
    }

    public void stopChronometer() {
        if(m_count_down_timer != null) {
            m_count_down_timer.cancel();
        }
        stopForeground(true);
    }

    private void setNotificationTimer(long time_remaining) {
        if(m_notification_builder != null) {
            String notification_content = String.format(
                    getString(R.string.notification_timer_text),
                    PPTActivity.PPTFragment.formatTimerText(time_remaining)
            );
            m_notification_builder.setContentText(notification_content);
            Notification notification = m_notification_builder.build();
            startForeground(FOREGROUND_NOTIFICATION_ID, notification);
        }
    }

    private void setForeground() {
        Intent resultIntent = new Intent(this, PPTActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        m_notification_builder.setSmallIcon(R.mipmap.ic_launcher);
        String notification_title = getString(R.string.notification_title_text);
        m_notification_builder.setContentTitle(notification_title);
        String notification_content = "At " + (new Date(m_target_time)).toString() + " playback will be paused.";
        m_notification_builder.setContentText(notification_content);
        m_notification_builder.setContentIntent(resultPendingIntent);
        Notification notification = m_notification_builder.build();
        startForeground(FOREGROUND_NOTIFICATION_ID, notification);
    }

    public class PPTBinder extends Binder {
        long getTargetTime() {
            return m_target_time;
        }

        long getRemainingTime() {
            return m_target_time - (new Date()).getTime();
        }
    }
}
