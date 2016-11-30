package com.hijacker;

/*
    Copyright (C) 2016  Christos Kyriakopoylos

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.path;
import static com.hijacker.MainActivity.shell;
import static com.hijacker.MainActivity.shell3_in;
import static com.hijacker.MainActivity.shell3_out;
import static com.hijacker.MainActivity.su_thread;

public class RestoreFirmwareDialog extends DialogFragment {
    View view;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        view = getActivity().getLayoutInflater().inflate(R.layout.restore_firmware, null);

        builder.setView(view);
        builder.setTitle(R.string.restore_firmware);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //close
            }
        });
        builder.setPositiveButton(R.string.restore, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });
        builder.setNeutralButton(R.string.find_firmware, new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i){}
        });
        return builder.create();
    }
    @Override
    public void onStart() {
        super.onStart();
        //Override positiveButton action to dismiss the fragment only when the directories exist, not on error
        AlertDialog d = (AlertDialog)getDialog();
        if(d != null) {
            final Button positiveButton = d.getButton(Dialog.BUTTON_POSITIVE);
            Button neutralButton = d.getButton(Dialog.BUTTON_NEUTRAL);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(shell==null){
                        su_thread.start();
                        try{
                            //Wait for su shells to spawn
                            su_thread.join();
                        }catch(InterruptedException ignored){}
                    }
                    String firm_location = ((EditText)view.findViewById(R.id.firm_location)).getText().toString();

                    File firm = new File(firm_location);
                    if(!firm.exists()){
                        Toast.makeText(getActivity().getApplicationContext(), R.string.dir_notfound_firm, Toast.LENGTH_SHORT).show();
                    }else if(!(new File(firm_location + "/fw_bcmdhd.bin").exists())){
                        Toast.makeText(getActivity().getApplicationContext(), R.string.firm_notfound, Toast.LENGTH_SHORT).show();
                    }else{
                        if(debug){
                            Log.d("RestoreFirmwareDialog", "Restoring firmware in " + firm_location);
                        }
                        shell3_in.print("busybox mount -o rw,remount,rw /system\n");
                        shell3_in.flush();

                        shell3_in.print("cp " + path + "/fw_bcmdhd.orig.bin " + firm_location + "/fw_bcmdhd.bin\n");
                        shell3_in.flush();
                        Toast.makeText(getActivity().getApplicationContext(), R.string.restored, Toast.LENGTH_SHORT).show();

                        shell3_in.print("busybox mount -o ro,remount,ro /system\n");
                        shell3_in.flush();
                        dismiss();
                    }
                }
            });
            neutralButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    positiveButton.setActivated(false);
                    if(shell==null){
                        su_thread.start();
                        try{
                            //Wait for su shells to spawn
                            su_thread.join();
                        }catch(InterruptedException ignored){}
                    }
                    ProgressBar progress = (ProgressBar)view.findViewById(R.id.install_firm_progress);
                    progress.setIndeterminate(true);
                    shell3_in.print("find /system/ -type f -name \"fw_bcmdhd.bin\"; echo ENDOFFIND\n");
                    shell3_in.flush();
                    try{
                        String buffer = null, lastline;
                        while(buffer==null){
                            buffer = shell3_out.readLine();
                        }
                        lastline = buffer;
                        while(!buffer.equals("ENDOFFIND")){
                            lastline = buffer;
                            buffer = shell3_out.readLine();
                        }
                        if(lastline.equals("ENDOFFIND")){
                            Toast.makeText(getActivity().getApplicationContext(), R.string.firm_notfound_bcm, Toast.LENGTH_LONG).show();
                        }else{
                            lastline = lastline.substring(0, lastline.length()-14);
                            ((EditText)view.findViewById(R.id.firm_location)).setText(lastline);
                        }
                    }catch(IOException ignored){}
                    progress.setIndeterminate(false);

                    positiveButton.setActivated(true);
                }
            });
        }
    }
}