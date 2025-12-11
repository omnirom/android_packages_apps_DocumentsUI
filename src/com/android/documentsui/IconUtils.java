/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.documentsui;

import static com.android.documentsui.util.FlagUtils.isUseMaterial3FlagEnabled;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.util.TypedValue;

import com.android.documentsui.base.UserId;
import com.android.documentsui.util.ColorUtils;

import java.util.HashMap;
import java.util.Map;

public class IconUtils {
    // key: drawable resource id, value: color attribute id
    private static final Map<Integer, Integer> sCustomIconColorMap = new HashMap<>();

    static {
        if (isUseMaterial3FlagEnabled()) {
            // Use Resources.getSystem().getIdentifier() here instead of
            // getRes(R.drawable.ic_doc_folder)
            // because com.android.internal.R is not public.
            sCustomIconColorMap.put(
                    Resources.getSystem().getIdentifier("ic_doc_folder", "drawable", "android"),
                    com.google.android.material.R.attr.colorPrimaryFixedDim);
            sCustomIconColorMap.put(
                    Resources.getSystem().getIdentifier("ic_doc_generic", "drawable", "android"),
                    com.google.android.material.R.attr.colorOutline);
            sCustomIconColorMap.put(
                    Resources.getSystem()
                            .getIdentifier("ic_doc_certificate", "drawable", "android"),
                    com.google.android.material.R.attr.colorOutline);
            sCustomIconColorMap.put(
                    Resources.getSystem().getIdentifier("ic_doc_codes", "drawable", "android"),
                    com.google.android.material.R.attr.colorOutline);
            sCustomIconColorMap.put(
                    Resources.getSystem().getIdentifier("ic_doc_contact", "drawable", "android"),
                    com.google.android.material.R.attr.colorOutline);
            sCustomIconColorMap.put(
                    Resources.getSystem().getIdentifier("ic_doc_font", "drawable", "android"),
                    com.google.android.material.R.attr.colorOutline);
        }
    }

    public static Drawable loadPackageIcon(Context context, UserId userId, String authority,
            int icon, boolean maybeShowBadge) {
        if (icon != 0) {
            final PackageManager pm = userId.getPackageManager(context);
            Drawable packageIcon = null;
            if (authority != null) {
                final ProviderInfo info = pm.resolveContentProvider(authority, 0);
                if (info != null) {
                    packageIcon = pm.getDrawable(info.packageName, icon, info.applicationInfo);
                }
            } else {
                packageIcon = userId.getDrawable(context, icon);
            }
            if (packageIcon != null && maybeShowBadge) {
                return userId.getUserBadgedIcon(context, packageIcon);
            } else {
                return packageIcon;
            }
        }

        return null;
    }

    public static Drawable loadMimeIcon(
            Context context, String mimeType, String authority, String docId, int mode) {
        return loadMimeIcon(context, mimeType);
    }

    /**
     * Load mime type drawable from system MimeIconUtils.
     * @param context activity context to obtain resource
     * @param mimeType specific mime type string of file
     * @return drawable of mime type files from system default
     */
    public static Drawable loadMimeIcon(Context context, String mimeType) {
        if (mimeType == null) return null;
        Icon icon = context.getContentResolver().getTypeInfo(mimeType).getIcon();
        Drawable drawable = icon.loadDrawable(context);
        // TODO(b/400263417): Remove this once RRO mime icons support dynamic colors.
        if (isUseMaterial3FlagEnabled()
                && drawable != null
                && sCustomIconColorMap.containsKey(icon.getResId())) {
            drawable.setTint(
                    ColorUtils.resolveMaterialColorAttribute(
                            context, sCustomIconColorMap.get(icon.getResId())));
        }
        return drawable;
    }

    public static Drawable applyTintColor(Context context, int drawableId, int tintColorId) {
        final Drawable icon = context.getDrawable(drawableId);
        return applyTintColor(context, icon, tintColorId);
    }

    public static Drawable applyTintColor(Context context, Drawable icon, int tintColorId) {
        icon.mutate();
        icon.setTintList(context.getColorStateList(tintColorId));
        return icon;
    }

    public static Drawable applyTintAttr(Context context, int drawableId, int tintAttrId) {
        final TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(tintAttrId, outValue, true);
        return applyTintColor(context, drawableId, outValue.resourceId);
    }
}
