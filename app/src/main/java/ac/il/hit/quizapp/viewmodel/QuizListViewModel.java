package ac.il.hit.quizapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import ac.il.hit.quizapp.interfaces.OnFirestoreTaskComplete;
import ac.il.hit.quizapp.firebase.repo.FirebaseRepository;
import ac.il.hit.quizapp.model.QuizListModel;

public class QuizListViewModel extends ViewModel implements OnFirestoreTaskComplete {

    //Mutable live data, extends from live data and is used to "SetValue" in Real Time and change text
    //accordingly to what appears at the DB
    private MutableLiveData<List<QuizListModel>> quizListModelData = new MutableLiveData<>();

    public LiveData<List<QuizListModel>> getQuizListModelData() {
        return quizListModelData;
    }

    private FirebaseRepository firebaseRepository = new FirebaseRepository(this);

    public QuizListViewModel() {
        firebaseRepository.getQuizData();
    }

    @Override
    public void quizListDataAdded(List<QuizListModel> quizListModelList) {
        quizListModelData.setValue(quizListModelList);
    }

    @Override
    public void onError(Exception e) {
        //need to handle exception
    }
}
