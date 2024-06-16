package com.example.shifttracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class GoalsCardFragment extends Fragment {
    ProgressBar progressMonthlyIncome, progressWeeklyHours;
    TextView tvIncomeProgress, tvHoursProgress;

    public GoalsCardFragment() {
        super(R.layout.goals_card);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.goals_card, container, false);

        progressMonthlyIncome = view.findViewById(R.id.progressMonthlyIncome);
        progressWeeklyHours = view.findViewById(R.id.progressWeeklyHours);
        tvIncomeProgress = view.findViewById(R.id.tvIncomeProgress);
        tvHoursProgress = view.findViewById(R.id.tvHoursProgress);

        updateGoalsProgress();

        return view;
    }

    private void updateGoalsProgress() {
        // Set goals as max values for progress bars
        progressMonthlyIncome.setMax(FirebaseManager.getUserInstance().getTargetMonthlyIncome());
        progressWeeklyHours.setMax(FirebaseManager.getUserInstance().getTargetWeeklyHours());

        // Get total income of this month
        int thisMonthIncome = (int) Math.floor(FirebaseManager.getThisMonthIncome());
        progressMonthlyIncome.setProgress(thisMonthIncome);

        tvIncomeProgress.setText(thisMonthIncome + "/" + progressMonthlyIncome.getMax());

        // Get total hours worked this week
        int thisWeekWorkingHours = (int) Math.floor(FirebaseManager.getThisWeekWorkingHours());
        progressWeeklyHours.setProgress(thisWeekWorkingHours);

        tvHoursProgress.setText(thisWeekWorkingHours + "/" + progressWeeklyHours.getMax());
    }
}
