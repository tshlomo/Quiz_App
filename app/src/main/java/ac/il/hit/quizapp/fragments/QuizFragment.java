package ac.il.hit.quizapp.fragments;


import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ac.il.hit.quizapp.R;
import ac.il.hit.quizapp.model.QuestionsModel;


public class QuizFragment extends Fragment implements View.OnClickListener {

    //Declaration
    private static final String TAG = "QUIZ_FRAGMENT_LOG";

    private NavController navController;

    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;

    private String currentUserId;

    private String quizName;
    private String quizId;

    //UI Elements
    private TextView quizTitle;
    private Button optionOneBtn;
    private Button optionTwoBtn;
    private Button optionThreeBtn;
    private Button nextBtn;
    private ImageButton closeBtn;
    private TextView questionFeedback;
    private TextView questionText;
    private TextView questionTime;
    private ProgressBar questionProgress;
    private TextView questionNumber;

    private CountDownTimer countDownTimer;

    private boolean canAnswer = false;
    private int currentQuestion = 0;

    private int correctAnswers = 0;
    private int wrongAnswers = 0;
    private int notAnswered = 0;

    //Firebase Data
    private List<QuestionsModel> allQuestionsList = new ArrayList<>();
    private long totalQuestionsToAnswer = 0L;
    private List<QuestionsModel> questionsToAnswer = new ArrayList<>();

    public QuizFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_quiz, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);

        firebaseAuth = firebaseAuth.getInstance();

        //get user id
        if(firebaseAuth.getCurrentUser() != null) {
            currentUserId = firebaseAuth.getCurrentUser().getUid();
        } else {
            //go back to home page
        }


        //initialize
        firebaseFirestore = FirebaseFirestore.getInstance();

        //UI initialization
        quizTitle = view.findViewById(R.id.quiz_title);
        optionOneBtn = view.findViewById(R.id.quiz_option_one);
        optionTwoBtn = view.findViewById(R.id.quiz_option_two);
        optionThreeBtn = view.findViewById(R.id.quiz_option_three);
        nextBtn = view.findViewById(R.id.quiz_next_btn);
        questionFeedback = view.findViewById(R.id.quiz_question_feedback);
        questionText = view.findViewById(R.id.quiz_question);
        questionTime = view.findViewById(R.id.quiz_question_time);
        questionProgress = view.findViewById(R.id.quiz_question_progress);
        questionNumber = view.findViewById(R.id.quiz_question_number);
        closeBtn = view.findViewById(R.id.quiz_close_btn);

        //get quizId
        quizId = QuizFragmentArgs.fromBundle(getArguments()).getQuizId();
        quizName = QuizFragmentArgs.fromBundle(getArguments()).getQuizName();
        totalQuestionsToAnswer = QuizFragmentArgs.fromBundle(getArguments()).getTotalQuestions();

        //get all questions from the quiz
        firebaseFirestore.collection("QuizList")
                .document(quizId).collection("Questions")
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()) {
                            allQuestionsList = task.getResult().toObjects(QuestionsModel.class);

                            //pickQuestions
                            pickQuestions();
                            loadUI();
                        } else {
                            //error getting questions
                            quizTitle.setText("Error : " + task.getException().getMessage());
                        }
                    }
                });


        //Set button click listeners
        optionOneBtn.setOnClickListener(this);
        optionTwoBtn.setOnClickListener(this);
        optionThreeBtn.setOnClickListener(this);

        nextBtn.setOnClickListener(this);

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showQuitDialog();
            }
        });
    }

    private void showQuitDialog() {
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        dialog.setContentView(R.layout.dialog_design);

        Button confirmBtn = dialog.findViewById(R.id.dialog_btn_confirm);
        Button cancelBtn = dialog.findViewById(R.id.dialog_btn_cancel);

        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //go to list page
                navController.navigate(R.id.action_quizFragment_to_listFragment);
                dialog.cancel();
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });


        dialog.show();

    }

    private void loadUI() {
        //quiz data loaded, load the UI
        questionText.setText(getString(R.string.first_question_load));

        quizTitle.setText(getString(R.string.starting_quiz_in));
        //start 5 sec countdown to let the user prepare
        new CountDownTimer(5 * 1000, 10) {
            @Override
            public void onTick(long millisUntilFinished) {
                //update time
                questionTime.setText(millisUntilFinished/1000 + "");

                Long percent = millisUntilFinished/(5*10);
                questionProgress.setProgress(percent.intValue());
            }

            @Override
            public void onFinish() {
                quizTitle.setText(quizName);
                //Enable Options
                enableOptions();

                //load first question
                loadQuestion(1);
            }
        }.start();


    }

    private void loadQuestion(int questionNum) {
        //set question number
        questionNumber.setText(String.valueOf(questionNum));

        //load question text
        questionText.setText(questionsToAnswer.get(questionNum-1).getQuestion());

        //load options
        optionOneBtn.setText(questionsToAnswer.get(questionNum-1).getOption_a());
        optionTwoBtn.setText(questionsToAnswer.get(questionNum-1).getOption_b());
        optionThreeBtn.setText(questionsToAnswer.get(questionNum-1).getOption_c());

        //Question Loaded, set can answer
        canAnswer = true;
        currentQuestion = questionNum;

        //start question timer
        startTimer(questionNum);

    }

    private void startTimer(int questionNumber) {

        //Set Timer Text
        final Long timerToAnswer = questionsToAnswer.get(questionNumber-1).getTimer();
        questionTime.setText(timerToAnswer.toString());

        //show timer progressBar
        questionProgress.setVisibility(View.VISIBLE);

        //start countdown
        countDownTimer = new CountDownTimer(timerToAnswer * 1000, 10) {
            @Override
            public void onTick(long millisUntilFinished) {
                //update time
                questionTime.setText(millisUntilFinished/1000 + "");

                Long percent = millisUntilFinished/(timerToAnswer*10);
                questionProgress.setProgress(percent.intValue());
            }

            @Override
            public void onFinish() {
                //times up, cant answer question anymore
                canAnswer = false;

                questionFeedback.setText(R.string.times_up_feedback);
                questionFeedback.setTextColor(getResources().getColor(R.color.colorPrimary,null));
                notAnswered++;
                showNextBtn();
            }
        };
        countDownTimer.start();
    }

    private void enableOptions() {
        //show all option buttons
        optionOneBtn.setVisibility(View.VISIBLE);
        optionTwoBtn.setVisibility(View.VISIBLE);
        optionThreeBtn.setVisibility(View.VISIBLE);

        //enable option buttons
        optionOneBtn.setEnabled(true);
        optionTwoBtn.setEnabled(true);
        optionThreeBtn.setEnabled(true);

        //hide feedback and next button
        questionFeedback.setVisibility(View.INVISIBLE);
        nextBtn.setVisibility(View.INVISIBLE);
        nextBtn.setEnabled(false);
    }

    private void pickQuestions() {
        for(int i = 0; i< totalQuestionsToAnswer; i++) {
            int randomNumber =  getRandomInt(allQuestionsList.size(),0);
            questionsToAnswer.add(allQuestionsList.get(randomNumber));
            allQuestionsList.remove(randomNumber);

            Log.d(TAG, "Question : " + questionsToAnswer.get(i).getQuestion());
        }
    }

    private int getRandomInt(int maximum, int minimum) {
        return ((int) (Math.random()*(maximum-minimum))) + minimum;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.quiz_option_one:
                verifyAnswer(optionOneBtn);
                break;
            case R.id.quiz_option_two:
                verifyAnswer(optionTwoBtn);
                break;
            case R.id.quiz_option_three:
                verifyAnswer(optionThreeBtn);
                break;
            case R.id.quiz_next_btn:
                if(currentQuestion == totalQuestionsToAnswer) {
                    //load results
                    submitResults();
                } else {
                    currentQuestion++;
                    loadQuestion(currentQuestion);
                    resetOptions();
                }
                break;
        }
    }

    private void submitResults() {
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("correct", correctAnswers);
        resultMap.put("wrong",wrongAnswers);
        resultMap.put("unanswered", notAnswered);

        firebaseFirestore.collection("QuizList")
                .document(quizId).collection("Results")
                .document(currentUserId).set(resultMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()) {
                    //go to results page
                    QuizFragmentDirections.ActionQuizFragmentToResultFragment action = QuizFragmentDirections.actionQuizFragmentToResultFragment();
                    action.setQuizId(quizId);
                    navController.navigate(action);
                } else {
                    //show error
                    quizTitle.setText(task.getException().getMessage());
                }
            }
        });
    }

    private void resetOptions() {
        optionOneBtn.setBackground(getResources().getDrawable(R.drawable.outline_light_btn_bg,null));
        optionTwoBtn.setBackground(getResources().getDrawable(R.drawable.outline_light_btn_bg,null));
        optionThreeBtn.setBackground(getResources().getDrawable(R.drawable.outline_light_btn_bg,null));

        optionOneBtn.setTextColor(getResources().getColor(R.color.colorLightText,null));
        optionTwoBtn.setTextColor(getResources().getColor(R.color.colorLightText,null));
        optionThreeBtn.setTextColor(getResources().getColor(R.color.colorLightText,null));

        questionFeedback.setVisibility(View.VISIBLE);
        nextBtn.setVisibility(View.INVISIBLE);
        nextBtn.setEnabled(false);
        questionFeedback.setText("");
    }

    private void verifyAnswer(Button selectedAnswerBtn) {


        //check answer
        if(canAnswer) {
            //set answer btn text color to black
            selectedAnswerBtn.setTextColor(getResources().getColor(R.color.colorDark,null));

            if(questionsToAnswer.get(currentQuestion-1).getAnswer().equals(selectedAnswerBtn.getText())) {
                //correct answer
                correctAnswers++;
                selectedAnswerBtn.setBackground(getResources().getDrawable(R.drawable.correct_answer_btn_bg,null));

                //set feedback text
                questionFeedback.setText(R.string.correct_answer_feedback);
                questionFeedback.setTextColor(getResources().getColor(R.color.colorPrimary,null));
            } else {
                //wrong answer
                wrongAnswers++;
                selectedAnswerBtn.setBackground(getResources().getDrawable(R.drawable.wrong_answer_btn_bg,null));

                //set feedback text
                String str =getString(R.string.wrong_answer_feedback) + "\n" + questionsToAnswer.get(currentQuestion-1).getAnswer();
                questionFeedback.setText(str);
                questionFeedback.setTextColor(getResources().getColor(R.color.colorAccent,null));
            }
            //set can answer to false
            canAnswer=false;

            //stop the timer
            countDownTimer.cancel();

            //show next button
            showNextBtn();
        }
    }

    private void showNextBtn() {
        if(currentQuestion == totalQuestionsToAnswer) {
            nextBtn.setText("Submit Results");
        }
        questionFeedback.setVisibility(View.VISIBLE);
        nextBtn.setVisibility(View.VISIBLE);
        nextBtn.setEnabled(true);
    }
}
