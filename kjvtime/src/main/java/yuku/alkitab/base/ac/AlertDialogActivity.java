package yuku.alkitab.base.ac;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import com.afollestad.materialdialogs.MaterialDialog;
import yuku.alkitab.base.App;
import yuku.alkitab.base.ac.base.BaseActivity;
import yuku.alkitab.debug.R;

/**
 * Use this class to show an alert dialog if you don't have an existing activity to
 * show the dialog on.
 *
 * This starts a transparent activity and then shows an alert dialog on top
 * of the transparent activity.
 */
public class AlertDialogActivity extends BaseActivity {

	/** Inputs */
	private static final String EXTRA_TITLE = "title";
	private static final String EXTRA_MESSAGE = "message";
	private static final String EXTRA_NEGATIVE = "negative";
	private static final String EXTRA_POSITIVE = "positive";
	private static final String EXTRA_INPUT_TYPE = "input_type";
	private static final String EXTRA_INPUT_HINT = "input_hint";
	private static final String EXTRA_LAUNCH = "launch";

	/** Output */
	public static final String EXTRA_INPUT = "input";

	public static Intent createOkIntent(final String title, final String message) {
		return new Intent(App.context, AlertDialogActivity.class)
			.putExtra(EXTRA_TITLE, title)
			.putExtra(EXTRA_MESSAGE, message);
	}

	public static Intent createAskIntent(final String title, final String message, final String negativeButtonText, final String positiveButtonText, final Intent launchWhenPositive) {
		return new Intent(App.context, AlertDialogActivity.class)
			.putExtra(EXTRA_TITLE, title)
			.putExtra(EXTRA_MESSAGE, message)
			.putExtra(EXTRA_NEGATIVE, negativeButtonText)
			.putExtra(EXTRA_POSITIVE, positiveButtonText)
			.putExtra(EXTRA_LAUNCH, launchWhenPositive);
	}

	public static Intent createInputIntent(final String title, final String message, final String negativeButtonText, final String positiveButtonText, final int inputType, final String inputHint) {
		return new Intent(App.context, AlertDialogActivity.class)
			.putExtra(EXTRA_TITLE, title)
			.putExtra(EXTRA_MESSAGE, message)
			.putExtra(EXTRA_NEGATIVE, negativeButtonText)
			.putExtra(EXTRA_POSITIVE, positiveButtonText)
			.putExtra(EXTRA_INPUT_TYPE, inputType)
			.putExtra(EXTRA_INPUT_HINT, inputHint);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final String title = getIntent().getStringExtra(EXTRA_TITLE);
		final String message = getIntent().getStringExtra(EXTRA_MESSAGE);
		final String negative = getIntent().getStringExtra(EXTRA_NEGATIVE);
		final String positive = getIntent().getExtras().getString(EXTRA_POSITIVE, getString(android.R.string.ok));
		final int inputType = getIntent().getIntExtra(EXTRA_INPUT_TYPE, 0);
		final String inputHint = getIntent().getStringExtra(EXTRA_INPUT_HINT);
		final Intent launch = getIntent().getParcelableExtra(EXTRA_LAUNCH);

		final MaterialDialog.Builder builder = new MaterialDialog.Builder(this);

		if (title != null) {
			builder.title(title);
		}

		if (message != null) {
			builder.content(message);
		}

		if (inputHint != null) {
			if (inputType != 0) {
				builder.inputType(inputType);
			}

			builder.input(inputHint, null, false, (dialog, input) -> {
				final Intent returnIntent = new Intent();
				returnIntent.putExtra(EXTRA_INPUT, input.toString());
				setResult(RESULT_OK, returnIntent);
				finish();
			});
		}

		builder.positiveText(positive);

		builder.callback(new MaterialDialog.ButtonCallback() {
			@Override
			public void onPositive(final MaterialDialog dialog) {
				if (inputHint == null) {
					final Intent returnIntent = new Intent();
					setResult(RESULT_OK, returnIntent);
					if (launch != null) {
						try {
							startActivity(launch);
						} catch (ActivityNotFoundException e) {
							new MaterialDialog.Builder(AlertDialogActivity.this)
								.content("Actvity was not found for intent: " + launch.toString())
								.positiveText(R.string.ok)
								.show();
						}
					}
					finish();
				}
			}

			@Override
			public void onNegative(final MaterialDialog dialog) {
				finish();
			}
		});

		if (negative != null) {
			builder.negativeText(negative);
		}

		builder.dismissListener(dialog -> finish());

		builder.show();
	}
}
