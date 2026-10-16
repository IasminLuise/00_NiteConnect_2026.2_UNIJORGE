import { Container } from "@cloudflare/containers";
import type { DurableObject } from "cloudflare:workers";

interface Env extends Cloudflare.Env {
	DB_URL: string;
	DB_USERNAME: string;
	DB_PASSWORD: string;
	FLYWAY_URL: string;
	ENTRA_ENABLED: "true";
	ENTRA_TENANT_ID: string;
	ENTRA_AUDIENCE: string;
	ENTRA_REQUIRED_SCOPE: string;
}

export class NiteConnectApi extends Container<Env> {
	defaultPort = 8080;
	sleepAfter = "10m";
	pingEndpoint = "/actuator/health";

	constructor(ctx: DurableObject["ctx"], env: Env) {
		super(ctx, env);
		this.envVars = {
			DB_URL: env.DB_URL,
			DB_USERNAME: env.DB_USERNAME,
			DB_PASSWORD: env.DB_PASSWORD,
			FLYWAY_URL: env.FLYWAY_URL,
			ENTRA_ENABLED: env.ENTRA_ENABLED,
			ENTRA_TENANT_ID: env.ENTRA_TENANT_ID,
			ENTRA_AUDIENCE: env.ENTRA_AUDIENCE,
			ENTRA_REQUIRED_SCOPE: env.ENTRA_REQUIRED_SCOPE,
		};
	}
}

export default {
	fetch(request: Request, env: Env): Promise<Response> {
		const configured = [
			env.DB_URL,
			env.DB_USERNAME,
			env.DB_PASSWORD,
			env.FLYWAY_URL,
			env.ENTRA_TENANT_ID,
			env.ENTRA_AUDIENCE,
			env.ENTRA_REQUIRED_SCOPE,
		].every((value) => typeof value === "string" && value.trim().length > 0);

		if (!configured) {
			return Promise.resolve(
				Response.json({ status: "unavailable" }, { status: 503 }),
			);
		}

		const api = env.NITE_CONNECT_API.getByName("staging");
		return api.fetch(request);
	},
} satisfies ExportedHandler<Env>;
