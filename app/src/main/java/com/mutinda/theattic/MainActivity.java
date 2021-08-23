package com.mutinda.theattic;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.mutinda.theattic.databinding.ActivityMainBinding;
import com.squareup.picasso.Picasso;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private DatabaseReference likesRef;
    private FirebaseAuth mAuth;
    private boolean likeChecker = false;
    private FirebaseRecyclerAdapter adapter;
    String currentUserID = null;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        // Initialize the recyclerView
        recyclerView = findViewById(R.id.recyclerView);
        // Reverse the layout so as to display the most recent post at the top
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);

        // Initialize the database reference where you will store likes
        likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
        // Get an instance of Firebase Authentication
        mAuth = FirebaseAuth.getInstance();
        // Get currently logged in user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // If user is not logged in refer them to the registerActivity
            Intent loginIntent = new Intent(MainActivity.this, RegisterActivity.class);
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginIntent);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        // Check to see if the user is logged in
        if (currentUser != null) {
            // if user is logged in, populate the UI with card views
            updateUI(currentUser);
            // Listen to the events on the adapter
            adapter.startListening();
        }
    }

    private void updateUI(FirebaseUser mCurrentUser) {
        // Create and initialize an instance of Query that retrieves all posts uploaded
        Query query = FirebaseDatabase.getInstance().getReference().child("Posts");
        // Create and initialize an instance of RecyclerOptions passing in your model class
        FirebaseRecyclerOptions<Attic> options = new FirebaseRecyclerOptions.Builder<Attic>().setQuery(query, new SnapshotParser<Attic>() {
            @NonNull
            @Override
            // Create  snapshot of your model
            public Attic parseSnapshot(@NonNull DataSnapshot snapshot) {
                return new Attic(snapshot.child("title").getValue().toString(),
                        snapshot.child("desc").getValue().toString(),
                        snapshot.child("postImage").getValue().toString(),
                        snapshot.child("displayName").getValue().toString(),
                        snapshot.child("profilePhoto").getValue().toString(),
                        snapshot.child("time").getValue().toString(),
                        snapshot.child("date").getValue().toString());
            }
        }).build();
        // Create a Firebase adapter passing in the model, and a viewHolder
        // Create a new ViewHolder as a public inner class that extends RecyclerView.Holder; outside the create, start and, updateUI methods.
        // Then implement the methods onCreateViewHolder nd onBindViewHolder
        // Complete all the steps in the AtticViewHolder before proceeding to the methods onCreateViewHolder, and onBindViewHolder
        adapter = new FirebaseRecyclerAdapter<Attic, AtticViewHolder>(options) {

            @NonNull
            @Override
            public AtticViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                // Inflate the layout where you have the card view items
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_items, parent, false);
                return new AtticViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull AtticViewHolder holder, int position, @NonNull Attic model) {
                // VERY IMPORTANT for ou to get the post key since we will use this to set likes and delete a particular post
                final String post_key = getRef(position).getKey();
                // Populate the card views with data
                holder.setTitle(model.getTitle());
                holder.setDesc(model.getDesc());
                holder.setPostImage(getApplicationContext(), model.getPostImage());
                holder.setUserName(model.getDisplayName());
                holder.setProfilePhoto(getApplicationContext(), model.getProfilePhoto());
                holder.setTime(model.getTime());
                holder.setDate(model.getDate());
                //Set a like on a particular post
                holder.setLikesButtonStatus(post_key);
                // Add an onClickListener on the particular post to allow opening this post on a different screen
                holder.post_Layout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Launch the screen singlePostActivity on clicking a  particular cardView item
                        // Create this activity using the empty activity template
                        Intent singleActivity = new Intent(MainActivity.this, SinglePostActivity.class);
                        singleActivity.putExtra("PostID", post_key);
                        startActivity(singleActivity);
                    }
                });
                // Set the onClickListener on the button for liking a post
                holder.likePostButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Initialize the like checker to true. We are using this boolean variable to determine if a post has been liked or disliked
                        // We declare this variable onto our activity class
                        likeChecker = true;
                        // Check he currently logged in user using their ID
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            currentUserID = user.getUid();
                        } else {
                            Toast.makeText(MainActivity.this, "Please Login", Toast.LENGTH_SHORT).show();
                        }
                        // Listen to changes in the likes database reference
                        likesRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (Objects.equals(likeChecker, true)) {
                                    // If the current post has a like, associated to the current logged in user and the user clicks on it again, remove the like. Basically, This means the user is disliking the post.
                                    if (snapshot.child(post_key).hasChild(currentUserID)) {

                                        likesRef.child(post_key).child(currentUserID).removeValue();
                                        likeChecker = false;

                                    } else {
                                        // Here the user is liking, set value on the like
                                        likesRef.child(post_key).child(currentUserID).setValue(true);
                                        likeChecker = false;
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }
                });

            }
        };
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            adapter.stopListening();
        }
    }

    private class AtticViewHolder extends RecyclerView.ViewHolder {
        // Declare the objects of the card view
        public TextView post_title, post_desc, postUserName, postTime, postDate, displayLikes;
        public ImageView post_image, user_image, likePostButton;
        public ImageButton commentPostButton;
        public LinearLayout post_Layout;
        //Declare an int variable that holds the count for likes
        int countLikes;
        // Declare a string variable to hold the user ID of currently logged in user
        String currentUserID;
        // Declare an instance of Firebase Authentication
        FirebaseAuth mAuth;
        // Declare a database reference where you are saving the likes
        DatabaseReference likesRef;
        public AtticViewHolder(@NonNull View itemView) {
            super(itemView);
            //Initialize the card view item objects
            post_title = itemView.findViewById(R.id.post_title_txtview);
            post_desc = itemView.findViewById(R.id.post_desc_txtview);
            post_image = itemView.findViewById(R.id.post_image);
            postUserName = itemView.findViewById(R.id.post_user);
            user_image = itemView.findViewById(R.id.userImage);
            postTime = itemView.findViewById(R.id.time);
            postDate = itemView.findViewById(R.id.date);
            post_Layout = itemView.findViewById(R.id.linear_layout_post);
            likePostButton = itemView.findViewById(R.id.like_button);
            commentPostButton = itemView.findViewById(R.id.comment);
            displayLikes = itemView.findViewById(R.id.likes_display);

            //Initialize a database reference where you will store the likes
            likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");

        }

        // Create your setters. You will use this setters in your onBindViewHolder method
        public void setTitle(String title) {
            post_title.setText(title);
        }

        public void setDesc(String desc) {
            post_desc.setText(desc);
        }

        public void setUserName(String userName) {
            postUserName.setText(userName);
        }

        public void setTime(String time) {
            postTime.setText(time);
        }

        public void setDate(String date) {
            postDate.setText(date);
        }

        public void setPostImage(Context ctx, String postImage) {
            Picasso.with(ctx).load(postImage).into(post_image);
        }

        public void setProfilePhoto(Context context, String profilePhoto) {
            Picasso.with(context).load(profilePhoto).into(user_image);
        }

        public void setLikesButtonStatus(final String post_key) {
            // We want to know who has liked a particular post, so let's get the user using their user_ID
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                currentUserID = user.getUid();
            } else {
                Toast.makeText(MainActivity.this, "Please Login", Toast.LENGTH_SHORT).show();
            }

            // Listen to changes in the database reference for likes
            likesRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // define post_key in the on BindViewHolder method
                    // check if a particular post has been liked
                    if (snapshot.child(post_key).hasChild(currentUserID)) {
                        // if liked get the number of likes
                        countLikes = (int) snapshot.child(post_key).getChildrenCount();
                        // check the image from initial dislike to like
                        likePostButton.setImageResource(R.drawable.like);
                        // count the likes and display them in the textView for likes
                        displayLikes.setText(Integer.toString(countLikes));
                    } else {
                        // If disliked, get the current number of likes
                        countLikes = (int) snapshot.child(post_key).getChildrenCount();
                        // Set the image resource to disliked
                        likePostButton.setImageResource(R.drawable.dislike);
                        // Display the current number of likes
                        displayLikes.setText(Integer.toString(countLikes));
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
            // implement the functionality of the add icon, so that the user on clicking  it launches the post activity
        } else if (id == R.id.action_add) {
            Intent postIntent = new Intent(MainActivity.this, PostActivity.class);
            startActivity(postIntent);
            // on clicking logout, log the user out
        } else if (id == R.id.action_logout) {
            mAuth.signOut();
            Intent logoutIntent = new Intent(MainActivity.this, RegisterActivity.class);
            logoutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(logoutIntent);
        }
        return super.onOptionsItemSelected(item);
    }
}