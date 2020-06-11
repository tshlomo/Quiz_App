package ac.il.hit.quizapp.interfaces;

import java.util.List;

import ac.il.hit.quizapp.model.QuizListModel;

public interface OnFirestoreTaskComplete {
    void quizListDataAdded(List<QuizListModel> quizListModelList);
    void onError(Exception e);
}
