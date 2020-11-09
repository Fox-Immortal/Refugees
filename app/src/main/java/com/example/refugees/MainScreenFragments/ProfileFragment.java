package com.example.refugees.MainScreenFragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.QuickContactBadge;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.refugees.HelperClasses.Address;
import com.example.refugees.HelperClasses.Validation;
import com.example.refugees.MainScreenActivity;
import com.example.refugees.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.IOException;

import br.com.simplepass.loading_button_lib.customViews.CircularProgressButton;
import de.hdodenhof.circleimageview.CircleImageView;

import static android.app.Activity.RESULT_OK;
import static com.example.refugees.MainScreenActivity.imageUri;
import static com.example.refugees.MainScreenActivity.imgChangedListener;
import static com.example.refugees.MainScreenActivity.navImg;
import static com.example.refugees.MainScreenActivity.navName;

public class ProfileFragment extends Fragment {
    public ProfileFragment() {
        // Required empty public constructor
    }

    private Context context;
    private static final int GALLERY_REQUEST_CODE = 1;
    private View view;
    private CircleImageView profileImg;
    private ImageView updateImgBtn;
    private TextInputLayout name, email, phone, addressG, addressC;
    private String _name, _phone, _addressG, _addressC;
    private SwitchCompat searchSwitch;
    private boolean _searchState;
    private CircularProgressButton updateBtn, saveBtn, cancelBtn;
    private LinearLayout updateBtnLayout;
    private FirebaseUser currentUser;
    private DatabaseReference userRef;
    private StorageReference mStorage;
    Validation validator;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_profile, container, false);
        InitializeFields();
        context = view.getContext();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        retrieveUserInfo();

        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                name.setEnabled(true);
                phone.setEnabled(true);
                addressG.setEnabled(true);
                addressC.setEnabled(true);
                searchSwitch.setEnabled(true);
                imgChangedListener = false;
                updateImgBtn.setVisibility(View.VISIBLE);
                email.setVisibility(View.GONE);
                updateBtn.setVisibility(View.GONE);
                updateBtnLayout.setVisibility(View.VISIBLE);
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                name.setEnabled(false);
                phone.setEnabled(false);
                addressG.setEnabled(false);
                addressC.setEnabled(false);
                searchSwitch.setEnabled(false);
                updateImgBtn.setVisibility(View.GONE);
                email.setVisibility(View.VISIBLE);
                updateBtn.setVisibility(View.VISIBLE);
                updateBtnLayout.setVisibility(View.GONE);
                retrieveUserInfo();
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isImgChanged() | isNameChanged() | isPhoneChanged() | isAddressChanged() | isSearchStateChanged()) {
                    cancelBtn.setEnabled(false);
                    saveBtn.startAnimation();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            retrieveUserInfo();
                            Toast.makeText(getContext(), getString(R.string.profile_updated), Toast.LENGTH_LONG).show();
                            saveBtn.revertAnimation();
                            saveBtn.setBackground(getResources().getDrawable(R.drawable.login_btn_bg));
                            name.setEnabled(false);
                            phone.setEnabled(false);
                            addressG.setEnabled(false);
                            addressC.setEnabled(false);
                            searchSwitch.setEnabled(false);
                            updateImgBtn.setVisibility(View.GONE);
                            email.setVisibility(View.VISIBLE);
                            updateBtn.setVisibility(View.VISIBLE);
                            updateBtnLayout.setVisibility(View.GONE);
                        }
                    }, 1500);
                }
            }
        });

        updateImgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);
            }
        });

    }

    private void InitializeFields() {
        profileImg = view.findViewById(R.id.profile_img);
        updateImgBtn = view.findViewById(R.id.update_img_btn);
        name = view.findViewById(R.id.profile_layout_name);
        email = view.findViewById(R.id.profile_layout_email);
        phone = view.findViewById(R.id.profile_layout_phone);
        addressG = view.findViewById(R.id.profile_layout_govrnator);
        addressC = view.findViewById(R.id.profile_layout_city);
        searchSwitch = view.findViewById(R.id.searchable_switch);
        updateBtnLayout = view.findViewById(R.id.update_btns_layout);
        updateBtn = view.findViewById(R.id.profile_update_info);
        saveBtn = view.findViewById(R.id.profile_save_changes);
        cancelBtn = view.findViewById(R.id.profile_cancel_update);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
        mStorage = FirebaseStorage.getInstance().getReference("ProfileImages");
        validator = new Validation(getActivity().getResources());
    }

    private void retrieveUserInfo() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Picasso.get().load(snapshot.child("image_url").getValue(String.class)).into(profileImg);
                    Picasso.get().load(snapshot.child("image_url").getValue(String.class)).into(navImg);
                    name.getEditText().setText(_name = snapshot.child("full_name").getValue(String.class));
                    navName.setText(_name = snapshot.child("full_name").getValue(String.class));
                    email.getEditText().setText(snapshot.child("email").getValue(String.class));
                    phone.getEditText().setText(_phone = snapshot.child("phone_no").getValue(String.class));
                    Address address = snapshot.child("address").getValue(Address.class);
                    addressG.getEditText().setText(_addressG = address.getGovernorate());
                    addressC.getEditText().setText(_addressC = address.getCity());
                    if (_searchState = snapshot.child("searchable").getValue(boolean.class)) {
                        searchSwitch.setChecked(true);
                    } else {
                        searchSwitch.setChecked(false);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("img pick", "onActivityResult: 1  " + requestCode);
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            Log.d("img pick", "onActivityResult: 2  "+requestCode);
            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1, 1)
                    .start(getActivity());
        }

    }

    public boolean isImgChanged() {
        if (imgChangedListener) {
            mStorage.child(currentUser.getUid() + ".jpg").putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    taskSnapshot.getStorage().getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()){
                                String downloadUrl = task.getResult().toString();
                                userRef.child("image_url").setValue(downloadUrl).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()){
                                            Log.d("profileUpdate", "img uploaded");
                                        }
                                    }
                                });
                            }
                        }
                    });
                }
            });
        }
        return imgChangedListener;
    }

    public boolean isNameChanged() {
        String newName = name.getEditText().getText().toString().trim();
        if (!_name.equals(newName)) {
            if (validator.validateName(name)) {
                userRef.child("full_name").setValue(newName).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d("profileUpdate", "name updated");
                        }
                    }
                });
            }
            return true;
        } else return false;
    }

    public boolean isPhoneChanged() {
        String newPhone = phone.getEditText().getText().toString().trim();
        if (!_phone.equals(newPhone)) {
            if (validator.validatePhoneNo(phone)) {
                userRef.child("phone_no").setValue(newPhone).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d("profileUpdate", "phone updated");
                        }
                    }
                });
            }
            return true;
        } else return false;
    }

    public boolean isAddressChanged() {
        String newAddressG = addressG.getEditText().getText().toString().trim();
        String newAddressC = addressC.getEditText().getText().toString().trim();
        if (!_addressG.equals(newAddressG) || !_addressC.equals(newAddressC)) {
            if (validator.validateNotEmpty(addressG) && validator.validateNotEmpty(addressC)) {
                Address newAddress = new Address(_addressG, _addressC);
                userRef.child("address").setValue(newAddress).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d("profileUpdate", "Address updated");
                        }
                    }
                });
            }
            return true;
        } else return false;
    }

    public boolean isSearchStateChanged() {
        boolean newState = searchSwitch.isChecked();
        if (_searchState != newState) {
            userRef.child("searchable").setValue(newState).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Log.d("profileUpdate", "searchable state updated");
                    }
                }
            });
            return true;
        } else return false;
    }

}