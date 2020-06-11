package ac.il.hit.quizapp.fragments;


import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.lang.ref.WeakReference;
import java.util.List;

import ac.il.hit.quizapp.R;
import ac.il.hit.quizapp.model.QuizListModel;
import ac.il.hit.quizapp.viewmodel.QuizListViewModel;


public class DetailsFragment extends Fragment implements View.OnClickListener {

    private NavController navController;
    private QuizListViewModel quizListViewModel;
    private int position;

    private ImageView detailsImage;
    private TextView detailsTitle;
    private TextView detailsDesc;
    private TextView detailsDiff;
    private TextView detailsQuestions;

    private Button detailsStartBtn;
    private String quizId;
    private String quizName;
    private long totalQuestions = 0;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;
    private TextView detailsScore;

    public DetailsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        navController = Navigation.findNavController(view);

        position = DetailsFragmentArgs.fromBundle(getArguments()).getPosition();

        //Initialize UI Elements
        detailsImage = view.findViewById(R.id.details_image);
        detailsTitle = view.findViewById(R.id.details_title);
        detailsDesc = view.findViewById(R.id.details_desc);
        detailsDiff = view.findViewById(R.id.details_difficulty_text);
        detailsQuestions = view.findViewById(R.id.details_questions_text);
        detailsScore = view.findViewById(R.id.details_score_text);

        detailsStartBtn = view.findViewById(R.id.details_start_btn);
        detailsStartBtn.setOnClickListener(this);

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        quizListViewModel = new ViewModelProvider(getActivity()).get(QuizListViewModel.class);
        quizListViewModel.getQuizListModelData().observe(getViewLifecycleOwner(), new Observer<List<QuizListModel>>() {
            @Override
            public void onChanged(List<QuizListModel> quizListModels) {
                DetailsTask detailsTask = new DetailsTask(quizListModels, firebaseAuth, firebaseFirestore);
                detailsTask.execute("QuizList","Results","correct","wrong","unanswered");
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.details_start_btn:
                DetailsFragmentDirections.ActionDetailsFragmentToQuizFragment action = DetailsFragmentDirections.actionDetailsFragmentToQuizFragment();
                action.setTotalQuestions(totalQuestions);
                action.setQuizName(quizName);
                action.setQuizId(quizId);
                navController.navigate(action);
                break;
        }
    }

    private class DetailsTask extends AsyncTask<String, Long, Void> {

        //weak ref
        private WeakReference<List<QuizListModel>> quizListModelWeakReference;
        private WeakReference<FirebaseAuth> firebaseAuthWeakReference;
        private WeakReference<FirebaseFirestore> firebaseFirestoreWeakReference;

        //constructor
        public DetailsTask(List<QuizListModel> listModels, FirebaseAuth firebaseAuthTask, FirebaseFirestore firebaseFirestoreTask) {
            quizListModelWeakReference = new WeakReference<List<QuizListModel>>(listModels);
            firebaseAuthWeakReference = new WeakReference<>(firebaseAuthTask);
            firebaseFirestoreWeakReference = new WeakReference<>(firebaseFirestoreTask);
        }

        @Override
        protected Void doInBackground(final String... strings) {
            if (quizListModelWeakReference == null)
                return null;

            FirebaseFirestore firebaseFirestoreTask = firebaseFirestoreWeakReference.get();
            FirebaseAuth firebaseAuthTask = firebaseAuthWeakReference.get();
            //get results
            firebaseFirestoreTask.collection(strings[0])
                    .document(quizId).collection(strings[1])
                    .document(firebaseAuthTask.getCurrentUser().getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.isSuccessful()) {
                        DocumentSnapshot result = task.getResult();
                        if(result != null && result.exists()) {
                            //get result
                            Long correct = result.getLong(strings[2]);
                            Long wrong = result.getLong(strings[3]);
                            Long missed =  result.getLong(strings[4]);

                            publishProgress(correct,wrong,missed);
                        } else {
                            //document doesn't exists, and result should stay N/A
                        }
                    }
                }
            });
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            List<QuizListModel> quizListModels = quizListModelWeakReference.get();

            Glide.with(getContext())
                    .load(quizListModels.get(position).getImage())
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_image)
                    .into(detailsImage);

            detailsTitle.setText(quizListModels.get(position).getName());
            detailsDesc.setText(quizListModels.get(position).getDesc());
            detailsDiff.setText(quizListModels.get(position).getLevel());
            detailsQuestions.setText(quizListModels.get(position).getQuestions() + "");

            //Assign value to quizId var
            quizId = quizListModels.get(position).getQuiz_id();
            quizName = quizListModels.get(position).getName();
            totalQuestions = quizListModels.get(position).getQuestions();
        }

        @Override
        protected void onProgressUpdate(Long... values) {
            super.onProgressUpdate(values);


            //calculate progress
            Long total = values[0] + values[1] + values[2];
            Long percent = (values[0] * 100)/total;

            detailsScore.setText(percent + "%");
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }
}
