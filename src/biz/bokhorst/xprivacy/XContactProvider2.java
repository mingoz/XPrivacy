package biz.bokhorst.xprivacy;

import android.content.ContentProvider;
import android.database.Cursor;
import android.os.Binder;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;

public class XContactProvider2 extends XHook {

	public final static String cPermissionName = "contacts";

	@Override
	protected void before(MethodHookParam param) throws Throwable {
		// Do nothing
	}

	@Override
	protected void after(MethodHookParam param) throws Throwable {
		// Check if allowed
		ContentProvider contentProvider = (ContentProvider) param.thisObject;
		if (!isAllowed(contentProvider.getContext(), Binder.getCallingUid(), cPermissionName)) {
			// Return empty cursor
			Cursor cursor = (Cursor) param.getResult();
			if (cursor != null)
				param.setResult(new XCursor(cursor));
		}
	}
}