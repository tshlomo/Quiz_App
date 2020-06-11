package ac.il.hit.quizapp.fragments;


import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.lang.ref.WeakReference;
import java.util.List;

import ac.il.hit.quizapp.R;


public class ResultFragment extends Fragment {

    private NavController navController;

    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;

    private String quizId;

    private String currentUserId;

    private TextView resultCorrect;
    private TextView resultWrong;
    private TextView resultMissed;

    private TextView resultPercent;
    private ProgressBar resultProgress;

    private Button resultHomeBtn;


    public ResultFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);

        firebaseAuth = FirebaseAuth.getInstance();

        //get user id
        if(firebaseAuth.getCurrentUser() != null) {
            currentUserId = firebaseAuth.getCurrentUser().getUid();
        } else {
            //go back to home page
        }


        //initialize
        firebaseFirestore = FirebaseFirestore.getInstance();

        quizId = ResultFragmentArgs.fromBundle(getArguments()).getQuizId();


        //initialize UI elements
        resultCorrect = view.findViewById(R.id.results_correct_text);
        resultWrong = view.findViewById(R.id.results_wrong_text);
        resultMissed = view.findViewById(R.id.results_missed_text);

        resultHomeBtn = view.findViewById(R.id.results_home_btn);
        resultHomeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navController.navigate(R.id.action_resultFragment_to_listFragment);
            }
        });

        resultPercent = view.findViewById(R.id.results_percent);
        resultProgress = view.findViewById(R.id.results_progress);


        ResultTask resultTask = new ResultTask(firebaseAuth,firebaseFirestore);
        resultTask.execute("QuizList", quizId, "Results", currentUserId, "correct", "wrong", "unanswered");

    }

    private class ResultTask extends AsyncTask<String,Long,Void> {
        //weak ref
        private WeakReference<FirebaseAuth> firebaseAuthWeakReference;
        private WeakReference<FirebaseFirestore> firebaseFirestoreWeakReference;

        //constructor
        public ResultTask(FirebaseAuth firebaseAuthTask, FirebaseFirestore firebaseFirestoreTask) {
            firebaseAuthWeakReference = new WeakReference<>(firebaseAuthTask);
            firebaseFirestoreWeakReference = new WeakReference<>(firebaseFirestoreTask);
        }

        @Override
        protected void onProgressUpdate(Long... values) {
            super.onProgressUpdate(values);


            //calculate progress
            Long total = values[0] + values[1] + values[2];
            Long percent = (values[0] * 100)/total;

            resultCorrect.setText(values[0].toString());
            resultWrong.setText(values[1].toString());
            resultMissed.setText(values[2].toString());

            resultPercent.setText(percent + "%");
            resultProgress.setProgress(percent.intValue());
        }

        @Override
        protected Void doInBackground(final String... strings) {

            FirebaseFirestore firebaseFirestoreTask = firebaseFirestoreWeakReference.get();
            FirebaseAuth firebaseAuthTask = firebaseAuthWeakReference.get();

            firebaseFirestoreTask.collection(strings[0])
                    .document(strings[1]).collection(strings[2])
                    .document(strings[3]).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.isSuccessful()) {
                        DocumentSnapshot result = task.getResult();

                        Long correct = result.getLong(strings[4]);
                        Long wrong = result.getLong(strings[5]);
                        Long missed =  result.getLong(strings[6]);

                        publishProgress(correct,wrong,missed);
                    } else {
                        //document doesn't exists, and result should stay N/A
                    }
                }
            });

            return null;
        }
    }
}
