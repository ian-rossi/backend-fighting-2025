package br.ianrossi.backendfighting2025.service;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@ApplicationScoped
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PendingPaymentsListener implements Runnable {

	BackendContextService service;
	PendingPaymentsService pendingPaymentsService;

	public void startup(@Observes final StartupEvent event) {
		new Thread(this).start();
	}

	@Override
	public void run() {
		while (!service.get().doesTheApplicationStopped().get()) {
			pendingPaymentsService.processPendingPayment();
		}
	}

	public void stop(@Observes final ShutdownEvent event) {
		service.get().doesTheApplicationStopped().set(true);
	}

}
