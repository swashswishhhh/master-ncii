package com.example.servermasterncii.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.servermasterncii.R;

public class QuestionAdapter extends ListAdapter<AdminQuestion, QuestionAdapter.QuestionViewHolder> {

    private final OnQuestionActionListener listener;

    public interface OnQuestionActionListener {
        void onEditQuestion(AdminQuestion question);
        void onDeleteQuestion(AdminQuestion question);
    }

    public QuestionAdapter(OnQuestionActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<AdminQuestion> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<AdminQuestion>() {
                @Override
                public boolean areItemsTheSame(@NonNull AdminQuestion oldItem,
                                               @NonNull AdminQuestion newItem) {
                    // Compare by Firestore document ID
                    return oldItem.getId() != null && oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull AdminQuestion oldItem,
                                                  @NonNull AdminQuestion newItem) {
                    // Compare actual content fields
                    return oldItem.getQuestionText().equals(newItem.getQuestionText()) &&
                            oldItem.getMissionId().equals(newItem.getMissionId()) &&
                            oldItem.getCorrectAnswer().equals(newItem.getCorrectAnswer());
                }
            };

    @NonNull
    @Override
    public QuestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_question, parent, false);
        return new QuestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuestionViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class QuestionViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvQuestionText;
        private final TextView tvCategory;        // reused for chapterId + missionId
        private final TextView tvCorrectAnswer;
        private final ImageButton btnEdit;
        private final ImageButton btnDelete;

        public QuestionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuestionText  = itemView.findViewById(R.id.tv_question_text);
            tvCategory      = itemView.findViewById(R.id.tv_category);
            tvCorrectAnswer = itemView.findViewById(R.id.tv_correct_answer);
            btnEdit         = itemView.findViewById(R.id.btn_edit);
            btnDelete       = itemView.findViewById(R.id.btn_delete);
        }

        public void bind(AdminQuestion question, OnQuestionActionListener listener) {
            tvQuestionText.setText(question.getQuestionText());

            // Display chapter + mission instead of the missing getCategory()
            tvCategory.setText(question.getChapterId() + "  /  " + question.getMissionId());

            tvCorrectAnswer.setText("✓  " + question.getCorrectAnswer());

            btnEdit.setOnClickListener(v   -> listener.onEditQuestion(question));
            btnDelete.setOnClickListener(v -> listener.onDeleteQuestion(question));
        }
    }
}