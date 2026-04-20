package com.example.servermasterncii;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.servermasterncii.databinding.ItemStepCardBinding;

import java.util.Collections;
import java.util.List;

/**
 * Adapter for the drag-to-reorder step sequencer.
 *
 * <p>Each card shows a positional badge (1–6) on the left, the step text in the
 * centre, and a drag-handle icon on the right. Long-pressing the card (or touching
 * the drag handle) starts a drag via the supplied {@link ItemTouchHelper}.</p>
 */
public class StepSequencerAdapter
        extends RecyclerView.Adapter<StepSequencerAdapter.StepViewHolder> {

    // ── State ──────────────────────────────────────────────────────────────
    private final List<String> steps;
    private ItemTouchHelper touchHelper;

    /** Indices (0-based) of cards that should be highlighted as wrong. */
    private boolean[] errorFlags;

    // ── Constructor ────────────────────────────────────────────────────────

    public StepSequencerAdapter(List<String> steps) {
        this.steps      = steps;
        this.errorFlags = new boolean[steps.size()];
    }

    // ── ItemTouchHelper wiring ─────────────────────────────────────────────

    public void attachTouchHelper(ItemTouchHelper helper) {
        this.touchHelper = helper;
    }

    // ── RecyclerView.Adapter ───────────────────────────────────────────────

    @NonNull
    @Override
    public StepViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStepCardBinding binding = ItemStepCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new StepViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StepViewHolder holder, int position) {
        ItemStepCardBinding b = holder.binding;

        // Badge shows current position (1-indexed)
        b.tvStepBadge.setText(String.valueOf(position + 1));

        // Step instruction text
        b.tvStepText.setText(steps.get(position));

        // Error highlight
        boolean isError = errorFlags[position];
        if (isError) {
            b.cardStep.setStrokeColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.quiz_wrong));
            b.cardStep.setStrokeWidth(dpToPx(holder, 2));
            b.cardStep.setCardBackgroundColor(
                    android.graphics.Color.parseColor("#1AF44336"));
            b.tvStepBadge.setBackgroundTintList(
                    ColorStateList.valueOf(
                            ContextCompat.getColor(holder.itemView.getContext(),
                                    R.color.quiz_wrong)));
            b.tvStepBadge.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(),
                            android.R.color.white));
        } else {
            b.cardStep.setStrokeColor(
                    ContextCompat.getColor(holder.itemView.getContext(),
                            R.color.cyber_neon_green));
            b.cardStep.setStrokeWidth(dpToPx(holder, 1));
            b.cardStep.setCardBackgroundColor(
                    android.graphics.Color.parseColor("#FF111620"));
            b.tvStepBadge.setBackgroundTintList(
                    ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#FF39FF7F")));
            b.tvStepBadge.setTextColor(
                    android.graphics.Color.parseColor("#FF0A0F0A"));
        }

        // Long-press on the whole card starts drag
        holder.itemView.setOnLongClickListener(v -> {
            if (touchHelper != null) touchHelper.startDrag(holder);
            return true;
        });

        // Touch on drag handle also starts drag immediately
        b.ivDragHandle.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                if (touchHelper != null) touchHelper.startDrag(holder);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return steps.size();
    }

    // ── Public helpers ─────────────────────────────────────────────────────

    /** Moves an item in the backing list (called by ItemTouchHelper). */
    public void moveItem(int from, int to) {
        Collections.swap(steps, from, to);
        notifyItemMoved(from, to);
        // Rebind badges for the affected range so numbers stay correct
        int start = Math.min(from, to);
        int end   = Math.max(from, to);
        notifyItemRangeChanged(start, end - start + 1);
    }

    /** Returns the current step list (in display order). */
    public List<String> getSteps() {
        return steps;
    }

    /**
     * Marks specific positions as wrong (red highlight).
     *
     * @param flags array of booleans, same length as the step list
     */
    public void setErrorFlags(boolean[] flags) {
        this.errorFlags = flags;
        notifyDataSetChanged();
    }

    /** Clears all error highlights. */
    public void clearErrors() {
        errorFlags = new boolean[steps.size()];
        notifyDataSetChanged();
    }

    // ── Utility ────────────────────────────────────────────────────────────

    private int dpToPx(@NonNull StepViewHolder holder, float dp) {
        float density =
                holder.itemView.getContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // ── ViewHolder ─────────────────────────────────────────────────────────

    static class StepViewHolder extends RecyclerView.ViewHolder {
        final ItemStepCardBinding binding;

        StepViewHolder(@NonNull ItemStepCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
