package com.apptasticmobile.notepad;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomappbar.BottomAppBar;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class MainActivity extends AppCompatActivity {
    EditText content;
    TextViewUndoRedo contentHistory;
    BottomAppBar bottomAppBar;
    private SharedPreferences mPreferences;
    private String sharedPrefFile = "com.apptasticmobile.notepad";

    public Uri currentFileUri;
    int CREATE_REQUEST_CODE = 1;
    int OPEN_REQUEST_CODE = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPreferences = getSharedPreferences(sharedPrefFile, MODE_PRIVATE);

        //get references
        content = findViewById(R.id.content);
        contentHistory = new TextViewUndoRedo(content);
        bottomAppBar = findViewById(R.id.bottomAppBar);
        setSupportActionBar(bottomAppBar);

        //if this app has received an implicit intent (to open text files)
        /*Intent intent = getIntent();    //intent that has triggered this activity
        String action = intent.getAction();
        if(action.compareTo(Intent.ACTION_VIEW) == 0){
            String scheme = intent.getScheme();

            if(scheme.compareTo(ContentResolver.SCHEME_FILE) == 0 || scheme.compareTo(ContentResolver.SCHEME_CONTENT) == 0){
                Uri uri = intent.getData();
                if (uri != null) {
                    currentFileUri = uri;
                    content.setText(readFile(currentFileUri));
                }
            }
        }*/

        //restore uri if saved
        if(savedInstanceState != null){
            currentFileUri = savedInstanceState.getParcelable("currentFileUri");
        }

        //set application theme
        if(mPreferences.getString("theme", "").equals("")){
            SharedPreferences.Editor preferencesEditor = mPreferences.edit();
            //if this is the first time this application is running
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                //android 10 or higher
                switch(getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK){
                    case Configuration.UI_MODE_NIGHT_NO:
                        preferencesEditor.putString("theme", "light");
                        break;
                    case Configuration.UI_MODE_NIGHT_YES:
                        preferencesEditor.putString("theme", "dark");
                        break;
                }
            } else {
                preferencesEditor.putString("theme", "light");
            }
            preferencesEditor.apply();
        }

        //apply theme
        if (mPreferences.getString("theme", "").equals("light"))
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        else if (mPreferences.getString("theme", "").equals("dark"))
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        //floating action button listener
        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean uriIsEmpty = currentFileUri == null;
                boolean fileHasBeenAltered = !uriIsEmpty && !readFile(currentFileUri).contentEquals(content.getText());
                boolean newFileNotYetSaved = uriIsEmpty && !content.getText().toString().equals("");

                if(fileHasBeenAltered || newFileNotYetSaved){
                    //if the text view has text

                    AlertDialog.Builder myAlertBuilder = new AlertDialog.Builder(MainActivity.this);
                    myAlertBuilder.setTitle("New");

                    if (fileHasBeenAltered)
                        myAlertBuilder.setMessage("Close ".concat(getFilename(currentFileUri)).concat(" without saving?"));
                    else if (newFileNotYetSaved)
                        myAlertBuilder.setMessage("Clear current text without saving?");

                    // Add the dialog buttons.
                    myAlertBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // User clicked yes button.
                            content.setText("");
                            currentFileUri = null;
                        }
                    });
                    myAlertBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // User clicked no button.

                        }
                    });

                    // Create and show the AlertDialog.
                    myAlertBuilder.show();
                } else {
                    content.setText("");
                    currentFileUri = null;
                }
            }
        });

        //bottom app bar listener
        bottomAppBar.setOnMenuItemClickListener(new BottomAppBar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                boolean uriIsEmpty = currentFileUri == null;
                boolean fileHasBeenAltered = !uriIsEmpty && !readFile(currentFileUri).contentEquals(content.getText());
                boolean newFileNotYetSaved = uriIsEmpty && !content.getText().toString().equals("");

                switch (item.getItemId()) {
                    //open
                    case R.id.open:
                        if(fileHasBeenAltered || newFileNotYetSaved){
                            AlertDialog.Builder myAlertBuilder = new AlertDialog.Builder(MainActivity.this);
                            myAlertBuilder.setTitle("Open");

                            if (fileHasBeenAltered)
                                myAlertBuilder.setMessage("Close ".concat(getFilename(currentFileUri)).concat(" without saving?"));
                            else if (newFileNotYetSaved)
                                myAlertBuilder.setMessage("Clear current text without saving?");

                            // Add the dialog buttons.
                            myAlertBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // User clicked yes button.
                                    openFile();
                                }
                            });
                            myAlertBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // User clicked no button.

                                }
                            });

                            // Create and show the AlertDialog.
                            myAlertBuilder.show();
                        } else {
                            openFile();
                        }
                        break;

                    //save
                    case R.id.save:
                        if (uriIsEmpty){
                            createFile("new.txt");
                        } else{
                            writeFile(currentFileUri, content.getText().toString());
                            Toast.makeText(getApplicationContext(), getFilename(currentFileUri).concat(" saved!"), Toast.LENGTH_SHORT).show();
                        }
                        break;

                    //save as
                    case R.id.saveAs:
                        createFile("new.txt");
                        break;

                    //undo
                    case R.id.undo:
                        contentHistory.undo();
                        break;

                    //redo
                    case R.id.redo:
                        contentHistory.redo();
                        break;

                    //change theme
                    case R.id.changeTheme:
                        SharedPreferences.Editor preferencesEditor = mPreferences.edit();
                        if (mPreferences.getString("theme", "").equals("light"))
                            preferencesEditor.putString("theme", "dark");
                        else if (mPreferences.getString("theme", "").equals("dark"))
                            preferencesEditor.putString("theme", "light");
                        preferencesEditor.apply();

                        //restart activity for changes to take effect
                        recreate();

                        break;
                }

                return true;
            }
        });
    }

    private void createFile(String filename){
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, filename);
        startActivityForResult(intent, CREATE_REQUEST_CODE);
    }

    private void openFile(){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        startActivityForResult(intent, OPEN_REQUEST_CODE);
    }

    private String readFile(Uri uri){
        InputStream stream;
        StringBuilder output = new StringBuilder();
        try{
            stream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

            //read all characters one by one
            while(reader.ready()){
                output.append((char) reader.read());
            }
        } catch (IOException e){
            e.printStackTrace();
        }

        return output.toString();
    }

    private void writeFile(Uri uri, String text){
        OutputStream stream;
        try{
            stream = getContentResolver().openOutputStream(uri);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream));
            writer.write(text);
            writer.flush();
            writer.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private String getFilename(Uri uri){
        Cursor cursor = getContentResolver().query(uri, null, null, null, null, null);
        String filename = "";
        try{
            if(cursor != null && cursor.moveToFirst()){
                filename = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return filename;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("currentFileUri", currentFileUri);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //populate items from R.menu.options into menu
        getMenuInflater().inflate(R.menu.options, menu);

        //update title of "change theme" option depending on the current theme
        if(mPreferences.getString("theme", "").equals("dark"))
            menu.findItem(R.id.changeTheme).setTitle("Light Mode");
        else
            menu.findItem(R.id.changeTheme).setTitle("Dark Mode");

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CREATE_REQUEST_CODE){
            if(resultCode == Activity.RESULT_OK){
                //if the user created the file
                if(data != null && data.getData() != null)
                    currentFileUri = data.getData();
                    writeFile(currentFileUri, content.getText().toString());
            } else if(resultCode == Activity.RESULT_CANCELED){
                //if the user closed the file picker

            }
        }
        else if(requestCode == OPEN_REQUEST_CODE){
            if(resultCode == Activity.RESULT_OK){
                //if the user opened the file
                if(data != null && data.getData() != null) {
                    currentFileUri = data.getData();
                    content.setText(readFile(currentFileUri));
                }
            } else if(resultCode == Activity.RESULT_CANCELED){
                //if the user closed the file picker

            }
        }
    }
}
