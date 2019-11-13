package com.vj.sudoku.v1;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;


public class Main extends Activity implements OnClickListener{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);


        //imposto i listener per i pulsanti
        View newButton = this.findViewById(R.id.new_button);
        newButton.setOnClickListener(this);
        View aboutButton = this.findViewById(R.id.about_button);
        aboutButton.setOnClickListener(this);
        View exitButton = this.findViewById(R.id.exit_button);
        exitButton.setOnClickListener(this);

    }

    public void onClick(View V) {

    	switch(V.getId()) {

    	// caso about
    	case R.id.about_button:
    		Intent i = new Intent(this, About.class);
    		startActivity(i);
    		break;
    	// caso exit
    	case R.id.exit_button:
    		finish();
    		break;
    	// si gioca
    	case R.id.new_button:
    		Intent intent = new Intent(this, Game.class);
    		startActivityForResult(intent, 123456789);
    		break;

    	}

    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data){
    	switch(requestCode){
    		case 123456789:
    				if(resultCode==RESULT_CANCELED)	finish();

    	}
    }


}