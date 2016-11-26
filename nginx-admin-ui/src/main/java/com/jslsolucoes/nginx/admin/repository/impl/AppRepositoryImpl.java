package com.jslsolucoes.nginx.admin.repository.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.jslsolucoes.nginx.admin.model.Nginx;
import com.jslsolucoes.nginx.admin.model.User;
import com.jslsolucoes.nginx.admin.repository.AppRepository;
import com.jslsolucoes.nginx.admin.repository.NginxRepository;
import com.jslsolucoes.nginx.admin.repository.UserRepository;

@RequestScoped
public class AppRepositoryImpl implements AppRepository {

	private UserRepository userRepository;
	private NginxRepository nginxRepository;

	public AppRepositoryImpl() {

	}

	@Inject
	public AppRepositoryImpl(UserRepository userRepository, NginxRepository nginxRepository) {
		this.userRepository = userRepository;
		this.nginxRepository = nginxRepository;

	}

	@Override
	public List<String> validateBeforeReconfigure(User user, String password, String passwordConfirm, String login,
			String bin, String config) {
		Set<String> errors = new HashSet<String>();

		errors.addAll(userRepository.validateBeforeChangeLogin(user, "password", login));
		errors.addAll(userRepository.validateBeforeChangePassword(user, "password", password, passwordConfirm));
		errors.addAll(nginxRepository.validateBeforeUpdate(new Nginx(bin)));
		if (new File(config).exists()) {
			errors.add("nginx.invalid.config.folder");
		}

		return new ArrayList<String>(errors);
	}

	@Override
	public void reconfigure(User user, String login, String password, String bin, String configHome)
			throws IOException {
		userRepository.changePassword(user, password);
		userRepository.changeLogin(user, login);
		nginxRepository.insert(new Nginx(bin));
		configure(configHome);
	}

	private void configure(String configHome) throws IOException {
		File home = new File(configHome);
		FileUtils.forceMkdir(home);
		nginxConf(home);
		nginxMimeType(home);
		FileUtils.forceMkdir(new File(home,"virtual-domain"));
		FileUtils.forceMkdir(new File(home,"upstream"));
		FileUtils.forceMkdir(new File(home,"log"));
		FileUtils.forceMkdir(new File(home,"process"));
	}

	private void nginxMimeType(File home) throws IOException {
		String template = IOUtils.toString(getClass().getResourceAsStream("/template/nginx/mime.types"),"UTF-8");
		FileUtils.writeStringToFile(new File(home,"mime.types"),template,"UTF-8");
	}

	private void nginxConf(File home) throws IOException {
		String template = IOUtils.toString(getClass().getResourceAsStream("/template/nginx/nginx.conf"),"UTF-8")
				.replaceAll("{{base}}", home.getAbsolutePath());
		FileUtils.writeStringToFile(new File(home,"nginx.conf"),template,"UTF-8");
	}

}
