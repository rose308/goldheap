package yuku.alkitab.base.dialog;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.text.TextUtils;
import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.MaterialDialog;
import yuku.alkitab.base.App;
import yuku.alkitab.base.IsiActivity;
import yuku.alkitab.base.S;
import yuku.alkitab.base.widget.AttributeView;
import yuku.alkitab.debug.R;
import yuku.alkitab.model.ProgressMark;

import java.util.Date;

public class ProgressMarkRenameDialog extends DialogFragment {
	public static final String TAG = ProgressMarkRenameDialog.class.getSimpleName();

	public interface Listener {
		void onOked();
		void onDeleted();
	}

	public static void show(final Activity activity, final ProgressMark progressMark, @NonNull final Listener listener) {
		final String caption = !TextUtils.isEmpty(progressMark.caption) ? progressMark.caption : activity.getString(AttributeView.getDefaultProgressMarkStringResource(progressMark.preset_id));

		new MaterialDialog.Builder(activity)
			.positiveText(R.string.ok)
			.neutralText(R.string.delete)
			.inputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT)
			.inputMaxLength(32)
			.input(activity.getString(R.string.pm_progress_name), caption, true, (dialog, text) -> {
				final String name = text.toString();
				if (TextUtils.isEmpty(name.trim())) {
					progressMark.caption = null;
				} else {
					progressMark.caption = name;
				}
				progressMark.modifyTime = new Date();
				S.getDb().insertOrUpdateProgressMark(progressMark);

				// Since updating database is the responsibility here,
				// announcing it will also be here.
				App.getLbm().sendBroadcast(new Intent(IsiActivity.ACTION_ATTRIBUTE_MAP_CHANGED));

				listener.onOked();
			})
			.callback(new MaterialDialog.ButtonCallback() {
				@Override
				public void onNeutral(final MaterialDialog dialog) {
					new AlertDialogWrapper.Builder(activity)
						.setMessage(TextUtils.expandTemplate(activity.getText(R.string.pm_delete_progress_confirm), caption))
						.setPositiveButton(R.string.ok, (_unused_, which) -> {
							progressMark.ari = 0;
							progressMark.caption = null;
							progressMark.modifyTime = new Date();
							S.getDb().insertOrUpdateProgressMark(progressMark);

							// Since updating database is the responsibility here,
							// announcing it will also be here.
							App.getLbm().sendBroadcast(new Intent(IsiActivity.ACTION_ATTRIBUTE_MAP_CHANGED));

							listener.onDeleted();
						})
						.setNegativeButton(R.string.cancel, null)
						.show();
				}
			})
			.show();
	}
}
