package app.organicmaps.navigator;

import androidx.fragment.app.Fragment;

import app.organicmaps.R;
import app.organicmaps.base.BaseToolbarActivity;

public class NavigatorSettingsActivity extends BaseToolbarActivity
{
  @Override
  protected Class<? extends Fragment> getFragmentClass()
  {
    return NavigatorSettingsFragment.class;
  }

  @Override
  protected int getToolbarTitle()
  {
    return R.string.navigator_settings;
  }
}
