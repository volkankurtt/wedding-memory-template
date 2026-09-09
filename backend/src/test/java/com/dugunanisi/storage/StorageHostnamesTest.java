package com.dugunanisi.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StorageHostnamesTest {

	@Test
	void rewritesApiGatewayHostToDedicatedStorageHost() {
		assertThat(StorageHostnames.dedicatedStorageOrigin("https://abcdxyz.supabase.co"))
				.isEqualTo("https://abcdxyz.storage.supabase.co");
		assertThat(StorageHostnames.rewriteToDedicatedStorage(
				"https://abcdxyz.supabase.co/storage/v1/object/upload/sign/guest-photos/a/original?token=t"))
				.isEqualTo("https://abcdxyz.storage.supabase.co/storage/v1/object/upload/sign/guest-photos/a/original?token=t");
	}

	@Test
	void doesNotDoubleRewriteStorageHost() {
		assertThat(StorageHostnames.rewriteToDedicatedStorage(
				"https://abcdxyz.storage.supabase.co/storage/v1/object/x"))
				.isEqualTo("https://abcdxyz.storage.supabase.co/storage/v1/object/x");
	}

	@Test
	void leavesLocalAndCustomHostsUnchanged() {
		assertThat(StorageHostnames.dedicatedStorageOrigin("http://127.0.0.1:54321"))
				.isEqualTo("http://127.0.0.1:54321");
		assertThat(StorageHostnames.rewriteToDedicatedStorage("https://storage.example/sign/p1"))
				.isEqualTo("https://storage.example/sign/p1");
	}
}
