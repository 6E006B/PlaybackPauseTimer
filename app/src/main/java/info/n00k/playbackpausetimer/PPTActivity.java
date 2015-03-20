package info.n00k.playbackpausetimer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;


public class PPTActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ppt);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PPTFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    public static class PPTFragment extends Fragment {

        Boolean m_bound;
        long m_remaining_time = 0;
        Timer m_timer = new Timer();
        CountDownTimer m_count_down_timer;
        TextView m_chronometer;

        public PPTFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_ppt, container, false);
            final EditText delay_field = (EditText)rootView.findViewById(R.id.editText);
            m_chronometer = (TextView)rootView.findViewById(R.id.timerTextView);
            Button start_button = (Button)rootView.findViewById(R.id.startButton);
            start_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        int delay = Integer.valueOf(delay_field.getText().toString());
                        Activity activity = PPTFragment.this.getActivity();
                        if(BuildConfig.DEBUG) {
                            Toast.makeText(activity, "PPT setting delay " + delay, Toast.LENGTH_SHORT).show();
                        }
                        Log.d("PPT", "setting delay "+delay);
                        Intent intent = new Intent(activity, PPTService.class);
                        intent.putExtra(PPTService.PPT_PAUSE_DELAY_ID, delay);
                        activity.startService(intent);
                        if (m_bound) {
                            activity.unbindService(m_connection);
                            m_bound = false;
                        }
                        activity.bindService(intent, m_connection, BIND_AUTO_CREATE | BIND_WAIVE_PRIORITY | BIND_ADJUST_WITH_ACTIVITY);
                    } catch (NumberFormatException e) {
                        Log.w("PPT", "user entered invalid number");
                    }
                }
            });
            Button stop_button = (Button)rootView.findViewById(R.id.stopButton);
            stop_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Activity activity = PPTFragment.this.getActivity();
                    if(BuildConfig.DEBUG) {
                        Toast.makeText(activity, "PPT stopping timer", Toast.LENGTH_SHORT).show();
                    }
                    Log.d("PPT", "stopping timer");
                    Intent intent = new Intent(activity, PPTService.class);
                    intent.putExtra(PPTService.PPT_PAUSE_DELAY_ID, 0);
                    activity.startService(intent);
                    if (m_bound) {
                        activity.unbindService(m_connection);
                        m_bound = false;
                    }
                    activity.bindService(intent, m_connection, BIND_AUTO_CREATE | BIND_WAIVE_PRIORITY | BIND_ADJUST_WITH_ACTIVITY);
                }
            });
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            m_remaining_time = 0;
            super.onAttach(activity);
            Intent intent = new Intent(activity, PPTService.class);
            activity.bindService(intent, m_connection, BIND_AUTO_CREATE | BIND_WAIVE_PRIORITY | BIND_ADJUST_WITH_ACTIVITY);
        }

        @Override
        public void onDestroy() {
            if (m_bound) {
                getActivity().unbindService(m_connection);
                m_bound = false;
            }
            super.onDestroy();
        }

        public static String formatTimerText(long milliseconds) {
            long seconds = milliseconds/1000;
            long minutes = seconds/60;
            long hours = minutes/60;
            long days = hours/24;
            seconds = seconds % 60;
            minutes = minutes % 60;
            hours = hours % 24;
            String formatted_string = "";
            if(days > 0) {
                formatted_string += String.valueOf(days) + "d ";
            }
            if(!formatted_string.equals("") || hours > 0) {
                formatted_string += String.format("%02d:", hours);
            }
            if(!formatted_string.equals("") || minutes > 0) {
                formatted_string += String.format("%02d:", minutes);
            }
            if(!formatted_string.equals("") || seconds > 0) {
                formatted_string += String.format("%02d", seconds);
            }
            return formatted_string;
        }

        public void startChronometer() {
            if(m_count_down_timer != null) {
                m_count_down_timer.cancel();
            }
            m_count_down_timer = new CountDownTimer(m_remaining_time, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    m_chronometer.setText(formatTimerText(millisUntilFinished));
                }

                @Override
                public void onFinish() {
                    m_chronometer.setText("0");
                }
            };
            m_count_down_timer.start();
        }

        public void stopChronometer() {
            if(m_count_down_timer != null) {
                m_count_down_timer.cancel();
            }
            m_chronometer.setText("0");
        }

        private ServiceConnection m_connection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName className,
                                           IBinder service) {
                PPTService.PPTBinder binder = (PPTService.PPTBinder) service;
                m_bound = true;
                m_remaining_time = binder.getRemainingTime();
                Log.d("PPT", "starting chronometer for " + m_remaining_time / 1000);
                if(m_remaining_time > 0) {
                    startChronometer();
                } else {
                    stopChronometer();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                m_bound = false;
            }
        };
    }
}
