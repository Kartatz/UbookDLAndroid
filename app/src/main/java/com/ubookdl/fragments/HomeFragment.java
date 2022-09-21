package com.ubookdl.fragments;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.Manifest;
import android.app.DownloadManager;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import org.json.JSONException;
import org.json.JSONObject;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.ubookdl.R;
import com.ubookdl.databinding.HomeFragmentBinding;

public class HomeFragment extends Fragment {
	
	private boolean loginRequired = true;
	private String token = null;
	
	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		final HomeFragmentBinding binding = HomeFragmentBinding.inflate(inflater, container, false);
		return binding.getRoot();
	}
	
	@Override
	public void onViewCreated(final View root, final Bundle savedInstanceState) {
		final FragmentActivity activity = getActivity();
		
		final Snackbar snackbar = Snackbar.make(activity.findViewById(android.R.id.content), null, Snackbar.LENGTH_INDEFINITE);
		final View snackbarView = snackbar.getView();
		final MaterialTextView snackbarText = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
		
		final CircularProgressIndicator progressBar = new CircularProgressIndicator(activity);
		progressBar.setIndeterminate(true);
		
		final ViewGroup layout = (ViewGroup) snackbarText.getParent();
		layout.addView(progressBar);
		
		// Error dialog
		final LayoutInflater layoutInflater = getLayoutInflater();
		final View errorDialogLayout = layoutInflater.inflate(R.layout.error_dialog, null);
		
		final MaterialTextView errorDialogText = errorDialogLayout.findViewById(R.id.error_dialog_text);
		
		final MaterialAlertDialogBuilder errorDialog = new MaterialAlertDialogBuilder(activity)
			.setTitle("Ocorreu um erro")
			.setView(errorDialogLayout)
			.setPositiveButton("Ok", null);
		
		// Success dialog
		final View successDialogLayout = layoutInflater.inflate(R.layout.success_dialog, null);
		
		final MaterialTextView successDialogText = successDialogLayout.findViewById(R.id.success_dialog_text);
		
		final MaterialAlertDialogBuilder successDialog = new MaterialAlertDialogBuilder(activity)
			.setTitle("Tudo pronto!")
			.setView(successDialogLayout)
			.setPositiveButton("Ok", null);
		
		// Login input and button
		final TextInputLayout usernameInputLayout = root.findViewById(R.id.username_input_layout);
		final TextInputEditText usernameInput = root.findViewById(R.id.username_input);
		
		final TextInputLayout passwordInputLayout = root.findViewById(R.id.password_input_layout);
		final TextInputEditText passwordInput = root.findViewById(R.id.password_input);
		
		final MaterialButton submitButton = root.findViewById(R.id.submit_button);
		
		// Shared preferences
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
		
		final Handler handler = new Handler(Looper.getMainLooper());
		
		final OkHttpClient client = new OkHttpClient();
		
		final String tk = preferences.getString("token", "");
		
		if (!TextUtils.isEmpty(tk)) {
			passwordInputLayout.setVisibility(View.GONE);
			
			usernameInputLayout.setHint("URL do livro");
			usernameInputLayout.setStartIconDrawable(R.drawable.http_icon);
			usernameInput.getText().clear();
			
			submitButton.setText("Baixar");
			
			this.loginRequired = false;
			this.token = token;
		}
		
		final Pattern urlPattern = Pattern.compile("https?://[a-z]+\\.ubook\\.com/(?:ebook|audiobook)/([0-9]+)(?:/[a-z0-9\\-]*)?", Pattern.CASE_INSENSITIVE);
		
		submitButton.setOnClickListener((final View view) -> {
			final String baseUrl = preferences.getString("backendBaseUrl", "https://www.ubook.com");
			
			if (this.loginRequired) {
				final String username = usernameInput.getText().toString();
				final String password = passwordInput.getText().toString();
				
				usernameInput.setError(null);
				
				if (TextUtils.isEmpty(username)) {
					usernameInput.requestFocus();
					usernameInput.setError("Por favor, insira seu usuário");
					return;
				}
				
				passwordInput.setError(null);
				
				if (TextUtils.isEmpty(password)) {
					passwordInput.requestFocus();
					passwordInput.setError("Por favor, insira uma senha");
					return;
				}
				
				progressBar.show();
				snackbarText.setText("Autenticando usuário");
				snackbar.show();
				
				new Thread(() -> {
					try {
						final Response response = client.newCall(
							new Request.Builder()
								.url(String.format("%s/backend/login", baseUrl))
								.post(
									new FormBody.Builder()
										.add("username", username)
										.add("password", password)
										.build()
								)
								.build()
						).execute();
						
						final JSONObject obj = new JSONObject(response.body().string());
						
						final boolean success = obj.getBoolean("success");
						final String token = (success ? obj.getJSONObject("data").getString("token") : null);
						
						handler.post(() -> {
							snackbar.dismiss();
							
							if (success) {
								final ViewParent parentView = successDialogLayout.getParent();
								
								if (parentView != null) {
									((ViewGroup) parentView).removeView(successDialogLayout);
								}
								
								successDialogText.setText("Você já pode começar a baixar seus livros.");
								successDialog.show();
								
								passwordInputLayout.setVisibility(View.GONE);
								
								usernameInputLayout.setHint("URL do livro");
								usernameInputLayout.setStartIconDrawable(R.drawable.http_icon);
								usernameInput.getText().clear();
								
								submitButton.setText("Baixar");
								
								preferences.edit()
									.putString("username", username)
									.putString("password", password)
									.putString("token", token)
									.commit();
								
								this.loginRequired = false;
							} else {
								final ViewParent parentView = errorDialogLayout.getParent();
								
								if (parentView != null) {
									((ViewGroup) parentView).removeView(errorDialogLayout);
								}
								
								errorDialogText.setText("Certifique-se de que as credenciais fornecidas estão corretas e/ou que a URL de acesso configurada corresponde a versão do site no qual sua assinatura está ativa.");
								errorDialog.show();
							}
						});
					} catch (final IOException | JSONException e) {
						handler.post(() -> {
							progressBar.hide();
							
							snackbarText.setText("Ocorreu um erro durante a comunicação com o servidor");
							snackbar.setDuration(Snackbar.LENGTH_LONG);
							snackbar.show();
						});
					}
				}).start();
			} else {
				final String inputUrl = usernameInput.getText().toString();
				
				usernameInput.setError(null);
				
				if (TextUtils.isEmpty(inputUrl)) {
					usernameInput.requestFocus();
					usernameInput.setError("Por favor, insira uma URL");
					return;
				}
				
				final Matcher matcher = urlPattern.matcher(inputUrl);
				
				if (!matcher.matches()) {
					usernameInput.requestFocus();
					usernameInput.setError("URL inválida ou não reconhecida");
					return;
				}
				
				progressBar.show();
				snackbarText.setText("Obtendo informações sobre o acesso");
				snackbar.show();
				
				final String bookId = matcher.group(1);
				final String token = preferences.getString("token", "");
				
				new Thread(() -> {
					try {
						Response response = client.newCall(
							new Request.Builder()
								.url(String.format("%s/backend/pingUserSession", baseUrl))
								.post(
									new FormBody.Builder()
										.add("token", token)
										.build()
								)
								.build()
						).execute();
						
						JSONObject tree = new JSONObject(response.body().string());
						boolean success = tree.getBoolean("success");
						
						if (!success) {
							handler.post(() -> {
								final String username = preferences.getString("username", "");
								final String password = preferences.getString("password", "");
								
								usernameInputLayout.setHint("Usuário");
								usernameInputLayout.setStartIconDrawable(R.drawable.person_icon);
								usernameInput.setText(username);
								usernameInput.setSelection(username.length());
								
								passwordInput.setText(password);
								passwordInput.setSelection(password.length());
								passwordInputLayout.setVisibility(View.VISIBLE);
								
								submitButton.setText("Acessar");
								
								progressBar.hide();
								snackbarText.setText("Por favor, refaça o login");
								snackbar.setDuration(Snackbar.LENGTH_LONG);
								snackbar.show();
								
								this.loginRequired = true;
							});
							
							return;
						}
						
						handler.post(() -> {
							snackbarText.setText("Obtendo informações sobre o livro");
							snackbar.show();
						});
						
						response = client.newCall(
							new Request.Builder()
								.url(String.format("%s/backend/product", baseUrl))
								.post(
									new FormBody.Builder()
										.add("id", bookId)
										.build()
								)
								.build()
						).execute();
						
						tree = new JSONObject(response.body().string());
						final JSONObject obj = tree.getJSONObject("data").getJSONObject("product");
						
						final String title = obj.getString("title");
						final String engine = obj.getString("engine");
						
						String filename = title;
						String endpoint = null;
						String documentFile = null;
						
						switch (engine) {
							case "ebook-epub":
								endpoint = String.format("%s/backend/getEpubFile", baseUrl);
								documentFile = "epub_file";
								filename += ".epub";
								break;
							case "ebook-pdf":
								endpoint = String.format("%s/backend/getPDFFile", baseUrl);
								documentFile = "pdf_file";
								filename += ".pdf";
								break;
							default:
								handler.post(() -> {
									snackbar.dismiss();
									
									final ViewParent parentView = errorDialogLayout.getParent();
									
									if (parentView != null) {
										((ViewGroup) parentView).removeView(errorDialogLayout);
									}
									
									errorDialogText.setText("O UbookDL ainda não suporta esse tipo de livro :(");
									errorDialog.show();
								});
								
								return;
						}
						
						response = client.newCall(
							new Request.Builder()
								.url(endpoint)
								.post(
									new FormBody.Builder()
										.add("id", bookId)
										.add("token", token)
										.build()
								)
								.build()
						).execute();
						
						tree = new JSONObject(response.body().string());
						success = tree.getBoolean("success");
						
						if (!success) {
							handler.post(() -> {
								snackbar.dismiss();
								
								final ViewParent parentView = errorDialogLayout.getParent();
								
								if (parentView != null) {
									((ViewGroup) parentView).removeView(errorDialogLayout);
								}
								
								errorDialogText.setText("Certifique-se de que sua conta possui uma assinatura premium ativa.");
								errorDialog.show();
							});
							
							return;
						}
						
						handler.post(() -> {
							snackbarText.setText(String.format("Baixando '%s'", title));
							snackbar.setDuration(Snackbar.LENGTH_SHORT);
							snackbar.show();
						});
						
						final String downloadUrl = tree.getJSONObject("data").getString(documentFile);
						
						final DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
						request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
						request.setTitle(String.format("Baixando '%s'", title));
						request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
						
						if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
							final DownloadManager downloadManager = (DownloadManager) activity.getSystemService(activity.DOWNLOAD_SERVICE);
							downloadManager.enqueue(request);
						} else {
							requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
						}
					} catch (final IOException | JSONException e) {
						handler.post(() -> {
							progressBar.hide();
							
							snackbarText.setText("Ocorreu um erro durante a comunicação com o servidor");
							snackbar.setDuration(Snackbar.LENGTH_LONG);
							snackbar.show();
						});
					}
				}).start();
			}
		});
		
	}

}
