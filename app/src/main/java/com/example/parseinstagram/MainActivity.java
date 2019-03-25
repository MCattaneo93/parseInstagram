package com.example.parseinstagram;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.File;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";
    public final static int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 1034;
    public String photoFileName = "photo.jpg";
    private File photoFile;
    @BindView(R.id.etDescription) public EditText etDescription;
    @BindView(R.id.btnCaptureImage) public Button btnCaptureImage;
    @BindView(R.id.ivPostImage) public ImageView ivPostImage;
    @BindView(R.id.btnSubmit) public Button btnSubmit;
    @BindView(R.id.btnLogout) public Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ParseUser[] currentUser = {ParseUser.getCurrentUser()};
        if(currentUser[0] == null)
            launchLoginActivity();

        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ParseUser.logOut();
                currentUser[0] = ParseUser.getCurrentUser();
                Toast.makeText(MainActivity.this, "Log out successful", Toast.LENGTH_SHORT).show();

                launchLoginActivity();

            }
        });


        btnCaptureImage.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                launchCamera();
            }
        });

        queryPosts();
        btnSubmit.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                String description = etDescription.getText().toString();
                ParseUser user = ParseUser.getCurrentUser();
                if(photoFile == null || ivPostImage.getDrawable() == null){
                    Log.e(TAG, "No photo to submit");
                    Toast.makeText(MainActivity.this, "There is no photo!", Toast.LENGTH_SHORT).show();
                }
                savePost(description, user, photoFile);
            }
        });
    }

    private void launchLoginActivity() {
        Log.d(TAG, "Navigating to Main Activity");
        Intent i = new Intent(this, LoginActivity.class);
        startActivity(i);
        finish();
    }

    private void launchCamera() {
        // create Intent to take a picture and return control to the calling application
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Create a File reference to access to future access
        photoFile = getPhotoFileUri(photoFileName);

        // wrap File object into a content provider
        // required for API >= 24
        // See https://guides.codepath.com/android/Sharing-Content-with-Intents#sharing-files-with-api-24-or-higher
        Uri fileProvider = FileProvider.getUriForFile(MainActivity.this, "com.codepath.fileprovider", photoFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider);

        // If you call startActivityForResult() using an intent that no app can handle, your app will crash.
        // So as long as the result is not null, it's safe to use the intent.
        if (intent.resolveActivity(getPackageManager()) != null) {
            // Start the image capture intent to take photo
            startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // by this point we have the camera photo on disk
                Bitmap takenImage = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                // RESIZE BITMAP, see section below
                // Load the taken image into a preview
                ivPostImage.setImageBitmap(takenImage);
            } else { // Result was a failure
                Toast.makeText(this, "Picture wasn't taken!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Returns the File for a photo stored on disk given the fileName
    public File getPhotoFileUri(String fileName) {
        // Get safe storage directory for photos
        // Use `getExternalFilesDir` on Context to access package-specific directories.
        // This way, we don't need to request external read/write runtime permissions.
        File mediaStorageDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), TAG);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()){
            Log.d(TAG, "failed to create directory");
        }

        // Return the file target for the photo based on filename
        File file = new File(mediaStorageDir.getPath() + File.separator + fileName);

        return file;
    }

    private void savePost(String description, ParseUser user, File photoFile) {
        Post post = new Post();
        post.setDescription(description);
        post.setUser(user);
        post.setImage(new ParseFile(photoFile));
        post.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null){
                    Log.d(TAG, "Error while saving");
                    e.printStackTrace();
                    return;
                }
                Log.d(TAG, "Success!!");
                etDescription.setText("");
                ivPostImage.setImageResource(0);
            }
        });
    }

    private void queryPosts() {
        ParseQuery<Post> postParseQuery = new ParseQuery<Post>(Post.class);
        postParseQuery.include(Post.KEY_USER);
        postParseQuery.findInBackground(new FindCallback<Post>() {
            @Override
            public void done(List<Post> posts, ParseException e) {
                if(e != null) {
                    Log.e(TAG, "Error with query");
                    e.printStackTrace();
                    return;
                }
                for(int i = 0; i < posts.size(); i++){
                    Post post = posts.get(i);
                    Log.d(TAG, "Post: " + post.getDescription() + ", Username: " +
                            post.getUser().getUsername());

                }

            }
        });
    }
}
