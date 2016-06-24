package org.makeathon.telepresenceslave;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class TestControlActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_control);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }


    public void onLeft(View v){
        SlaveService service = SlaveService.getInstance();
        if (service != null){
            service.onLeft();
        }
    }

    public void onRight(View v){
        SlaveService service = SlaveService.getInstance();
        if (service != null){
            service.onRight();
        }
    }

    public void onPoke(View v){
        SlaveService service = SlaveService.getInstance();
        if (service != null){
            service.onPoke();
        }
    }

    public void onForward(View v){
        SlaveService service = SlaveService.getInstance();
        if (service != null){
            service.onForward();
        }
    }

    public void onBackward(View v){
        SlaveService service = SlaveService.getInstance();
        if (service != null){
            service.onBackward();
        }
    }

    public void onSlap(View v){
        SlaveService service = SlaveService.getInstance();
        if (service != null){
            service.onSlap();
        }
    }



}
