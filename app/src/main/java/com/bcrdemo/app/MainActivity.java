package com.bcrdemo.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_TAKE_PHOTO = 1;
    String mCurrentPhotoPath;
    String[] websiteMatches = new String[]{"ww.","vw.","www."};
    boolean hasGotEmailID = false;
    boolean hasGotWebsite = false;
    boolean hasGotName = false;
    boolean hasGotNumber = false;
    HashSet<String> hsNumbers;
    HashMap<String, String> hmCardInfo;
    EditText edtName,edtEmail,edtPhone,edtWebsite,edtAddress;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        checkCameraPermission();
        hmCardInfo=new HashMap<>();
    }

    private void initViews() {
        edtName=findViewById(R.id.edtName);
        edtEmail=findViewById(R.id.edtEmail);
        edtPhone=findViewById(R.id.edtPhone);
        edtWebsite=findViewById(R.id.edtWebsite);
        edtAddress=findViewById(R.id.edtAddress);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED){
            requestPermissions(new String[]{Manifest.permission.CAMERA},
                    20);
        }else {
            openCamera();
        }

    }

    private void openCamera(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.bcrdemo.app.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = null;
        try {
            image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 20) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera Permission has been denied.", Toast.LENGTH_LONG).show();
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {

            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;
            // Determine how much to scale down the image
            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inPurgeable = true;
            Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

            String txt = getTextFromImage(bitmap);
            System.out.println("text"+txt);

            String name=  hmCardInfo.get("name");
            String email=  hmCardInfo.get("email");
            String number= hmCardInfo.get("number");
            String sDesignation = hmCardInfo.get("designation");
            String sAddress = hmCardInfo.get("address");
            String website=hmCardInfo.get("website");
            System.out.println("Address"+sAddress+sDesignation);


            edtName.setText(name);
            edtEmail.setText(email);
            edtPhone.setText(number);
           edtWebsite.setText(website);
           edtAddress.setText(sAddress);
        }
    }

    private String getTextFromImage(Bitmap bitmap){
        String str= "";
        TextRecognizer tr = new TextRecognizer.Builder(getApplicationContext()).build();
        if(!tr.isOperational())
            Log.e("ERROR", "Detector dependencies are not yet available");
        else{
            Frame frame =  new Frame.Builder().setBitmap(bitmap).build();
            SparseArray<TextBlock> items = tr.detect(frame);

            for(int i=0; i<items.size(); i++){
                TextBlock item = items.valueAt(i);
                str = item.getValue();

                extractInfo(str);
            }
        }
        return str;
    }

    void extractInfo(String text) {
        String name;
        StringBuilder sb = new StringBuilder();
        String email;
        String firstLine;
        String secondLine = null;
        String thirdLine = null;
        String fourthLine = null;
        String fifthLine = null;
        String[] lines = text.split(Objects.requireNonNull(System.getProperty("line.separator")));
        String[] matches = new String[]{"Email:", "Email", "email", "E-mail", "e-mail", "-mail"};


        ArrayList<String> alText = new ArrayList<>(Arrays.asList(lines));

        for (int i = 0; i < alText.size(); i++) {
            String sSingleLine = alText.get(i);

            if (sSingleLine != null)
                if (!hasGotWebsite && stringContainsItemFromList(sSingleLine, websiteMatches)){
                    Toast.makeText(getApplicationContext(), "yes website found", Toast.LENGTH_SHORT).show();
                    hmCardInfo.put("website", sSingleLine);
                    hasGotWebsite = true;
                } else
                if (!hasGotEmailID && sSingleLine.contains("@")) {
                    for (String s : matches) {
                        if (sSingleLine.contains(s)) {
                            email = sSingleLine.replace(s, "");
                            sSingleLine = email.replaceAll("[-+^:,]", "").trim();
                            break;
                        }
                    }

                    hmCardInfo.put("email", sSingleLine);
                    hasGotEmailID = true;
                } else {
                    if (!hasGotName && !sSingleLine.contains("@") && !sSingleLine.contains(",") && !sSingleLine.contains(".com")) {
                        name = sSingleLine.replace(".", "");
                        //sb.append(name);
                        hmCardInfo.put("name", name);

                        hasGotName = true;
                    } else {
                        identifyMultipleNumbersWithEmail(sSingleLine);
                    }
                }
        }

        firstLine = lines[0];

        if (lines.length > 1)
            secondLine = lines[1];
        if (lines.length > 2)
            thirdLine = lines[2];
        if (lines.length > 3)
            fourthLine = lines[3];
        if (lines.length > 4)
            fifthLine = lines[4];

        if (!hasGotEmailID && firstLine.contains("@")) {

            for (String s : matches) {
                if (firstLine.contains(s)) {
                    email = firstLine.replace(s, "");
                    firstLine = email.replaceAll("[-+^:,]", "").trim();
                    break;
                }
            }

            hmCardInfo.put("email", firstLine);
            hasGotEmailID = true;
        } else {
            if (!hasGotName && !firstLine.contains("@") && !firstLine.contains(",") && !firstLine.contains(".com")) {
                name = firstLine.replace(".", "");
                //sb.append(name);

                hmCardInfo.put("name", name);
                hasGotName = true;
            } else {
                identifyMultipleNumbersWithEmail(firstLine);
                String s1 = firstLine.replaceAll("[^0-9]", "");
                if (!hasGotNumber && s1.length() >= 10) {
                    if (s1.length() == 12) {
                        s1 = "+" + s1;
                    } else if (s1.length() > 12) {
                        identifyMultipleNumbersWithEmail(firstLine);
                    }

                    hmCardInfo.put("number", s1);
                    //hasGotNumber = true;
                }
            }
        }
        if (secondLine != null)
            if (!hasGotEmailID && secondLine.contains("@")) {
                for (String s : matches) {
                    if (secondLine.contains(s)) {
                        email = secondLine.replace(s, "");
                        secondLine = email.replaceAll("[-+^:,]", "").trim();
                        break;
                    }
                }
                hmCardInfo.put("email", secondLine);
                hasGotEmailID = true;
            } else {
                if (!hasGotName && !secondLine.contains("@") && !secondLine.contains(",") && !secondLine.contains(".com")) {
                    name = secondLine.replace(".", "");
                    //sb.append(" ").append(name);
                    hmCardInfo.put("name", name);

                    hasGotName = true;
                } else {
                    identifyMultipleNumbersWithEmail(secondLine);
                    String s1 = secondLine.replaceAll("[^0-9]", "");
                    if (!hasGotNumber && s1.length() >= 10) {
                        if (s1.length() == 12) {
                            s1 = "+" + s1;
                        } else if (s1.length() > 12) {
                            identifyMultipleNumbersWithEmail(secondLine);
                        }
                        hmCardInfo.put("number", s1);
                        //hasGotNumber = true;
                    }
                }
            }
        if (thirdLine != null)
            if (!hasGotEmailID && thirdLine.contains("@")) {
                for (String s : matches) {
                    if (thirdLine.contains(s)) {
                        email = thirdLine.replace(s, "");
                        thirdLine = email.replaceAll("[-+^:,]", "").trim();
                        break;
                    }
                }
                hmCardInfo.put("email", thirdLine);
                hasGotEmailID = true;
            } else {
                if (!hasGotName && !thirdLine.contains("@") && !thirdLine.contains(",") && !thirdLine.contains(".com")) {
                    name = thirdLine.replace(".", "");
                    //sb.append(" ").append(name);
                    hmCardInfo.put("name", name);
                    hasGotName = true;
                } else {
                    identifyMultipleNumbersWithEmail(thirdLine);
                    String s1 = thirdLine.replaceAll("[^0-9]", "");
                    if (!hasGotNumber && s1.length() >= 10) {
                        if (s1.length() == 12) {
                            s1 = "+" + s1;
                        } else if (s1.length() > 12) {
                            identifyMultipleNumbersWithEmail(thirdLine);
                        }
                        hmCardInfo.put("number", s1);
                        //hasGotNumber = true;
                    }
                }
            }
        if (fourthLine != null)
            if (!hasGotEmailID && fourthLine.contains("@")) {
                for (String s : matches) {
                    if (fourthLine.contains(s)) {
                        email = fourthLine.replace(s, "");
                        fourthLine = email.replaceAll("[-+^:,]", "").trim();
                        break;
                    }
                }
                hmCardInfo.put("email", fourthLine);
                hasGotEmailID = true;
            } else {
                if (!hasGotName && !fourthLine.contains("@") && !fourthLine.contains(",") && !fourthLine.contains(".com")) {
                    name = fourthLine.replace(".", "");
                    //sb.append(" ").append(name);
                    hmCardInfo.put("name", name);
                    hasGotName = true;
                } else {
                    identifyMultipleNumbersWithEmail(fourthLine);
                }
            }

        if (fifthLine != null)
            if (!hasGotEmailID && fifthLine.contains("@")) {
                for (String s : matches) {
                    if (fifthLine.contains(s)) {
                        email = fifthLine.replace(s, "");
                        fifthLine = email.replaceAll("[-+^:,]", "").trim();
                        break;
                    }
                }
                hmCardInfo.put("email", fifthLine);
                hasGotEmailID = true;
            } else {
                if (!hasGotName && !fifthLine.contains("@") && !fifthLine.contains(",") && !fifthLine.contains(".com")) {
                    name = fifthLine.replace(".", "");
                    //sb.append(" ").append(name);
                    hmCardInfo.put("name", name);
                    hasGotName = true;
                } else {
                    identifyMultipleNumbersWithEmail(fifthLine);
                }
            }
    }

    public static boolean stringContainsItemFromList(String inputStr, String[] items) {
        for (String item : items) {
            if (inputStr.contains(item)) {
                return true;
            }
        }
        return false;
    }

    private void identifyMultipleNumbersWithEmail(String str) {
        String sResultByComma[];
        //String sResultBySpace[];
        sResultByComma = str.split(",");
        //sResultBySpace = str.split(" ");
        StringBuilder sb;

        if (sResultByComma.length > 1) {
            hsNumbers = new HashSet<>(Arrays.asList(sResultByComma));
            ArrayList<String> alEmailCheck = new ArrayList<>(hsNumbers);
            for (int i = 0; i < alEmailCheck.size(); i++) {
                String sEmailCheck = alEmailCheck.get(i);
                if (!hasGotWebsite && stringContainsItemFromList(sEmailCheck, websiteMatches)){
                    Toast.makeText(getApplicationContext(), "yes website found", Toast.LENGTH_SHORT).show();
                    hmCardInfo.put("website", sEmailCheck);
                    hasGotWebsite = true;
                } else
                if (sEmailCheck.contains("@")) {
                    String sFinalEmail = sEmailCheck.replaceAll("[-+^:,]", "").trim();
                    hmCardInfo.put("email", sFinalEmail);
                    hasGotEmailID = true;
                }
                String s1 = sEmailCheck.replaceAll("[^0-9]", "").trim();

                if (s1.length()>9 && s1.length() <13) {
                    if(s1.length()== 12) {
                        s1 = "+" + s1;
                    }
                    if (hmCardInfo.containsKey("number")) {
                        sb = new StringBuilder();
                        sb.append(hmCardInfo.get("number")).append(",");
                        sb.append(s1);
                        hmCardInfo.put("number", sb.toString());
                    } else {
                        hmCardInfo.put("number", s1);
                    }
                }
            }
        } else if (sResultByComma.length == 1) {
            String sEmailCheck = sResultByComma[0];
            if (sEmailCheck.contains("@")) {
                String sFinalEmail = sEmailCheck.replaceAll("[-+^:,]", "").trim();
                hmCardInfo.put("email", sFinalEmail);
                hasGotEmailID = true;
            }
            String s1 = sEmailCheck.replaceAll("[^0-9]", "").trim();


            if (s1.length()>9 && s1.length() <13) {
                if(s1.length()== 12) {
                    s1 = "+" + s1;
                }
                if (hmCardInfo.containsKey("number")) {
                    sb = new StringBuilder();
                    sb.append(hmCardInfo.get("number")).append(",");
                    sb.append(s1);
                    hmCardInfo.put("number", sb.toString());
                } else {
                    hmCardInfo.put("number", s1);
                }
            }
        }

    }
}
