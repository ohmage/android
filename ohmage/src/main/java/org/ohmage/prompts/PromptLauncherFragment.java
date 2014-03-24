package org.ohmage.prompts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.ohmage.app.R;
import org.ohmage.prompts.AnswerablePrompt.AnswerablePromptFragment;

/**
* Created by cketcham on 3/24/14.
*/
public abstract class PromptLauncherFragment<T extends AnswerablePrompt>
        extends AnswerablePromptFragment<T>
        implements View.OnClickListener {

    @Override
    public void onCreatePromptView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.prompt_launch, container, true);
        Button launch = (Button) view.findViewById(R.id.launch);
        launch.setText(getLaunchButtonText());
        launch.setOnClickListener(this);
    }

    protected abstract String getLaunchButtonText();
}
