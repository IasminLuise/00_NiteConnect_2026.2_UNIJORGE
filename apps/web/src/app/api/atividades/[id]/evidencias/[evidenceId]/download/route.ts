import { getAuthenticatedNiteSession } from "../../../../../../../lib/auth/server-session";
import { downloadActivityEvidence } from "../../../../../../../lib/activities/mutations";
import { getActivityEvidenceById } from "../../../../../../../lib/activities/data";

export const dynamic = "force-dynamic";

type RouteContext = { params: Promise<{ id: string; evidenceId: string }> };
const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function safeFilename(value: string): string {
  return value.normalize("NFKC").replace(/[\\/\u0000-\u001f\u007f]/g, "_").replace(/^\.+/, "").slice(0, 160) || "evidencia";
}

function disposition(filename: string): string {
  const safe = safeFilename(filename);
  const fallback = safe.replace(/[^\x20-\x7e]|["\\;]/g, "_").replace(/^[. ]+|[. ]+$/g, "") || "evidencia";
  const encoded = encodeURIComponent(safe).replace(/[!'()*]/g, (character) => `%${character.charCodeAt(0).toString(16).toUpperCase()}`);
  return `attachment; filename="${fallback}"; filename*=UTF-8''${encoded}`;
}

function response(status: number): Response {
  return new Response(null, { status, headers: { "Cache-Control": "private, no-store", "X-Content-Type-Options": "nosniff" } });
}

export async function GET(_request: Request, { params }: RouteContext): Promise<Response> {
  const { id, evidenceId } = await params;
  if (!uuidPattern.test(id) || !uuidPattern.test(evidenceId)) return response(404);
  const session = await getAuthenticatedNiteSession();
  if (!session) return response(401);

  const evidence = await getActivityEvidenceById(session.bearer, id, evidenceId);
  if (evidence.issue) return response(evidence.issue === "access" ? 403 : evidence.issue === "not-found" ? 404 : 503);
  if (!evidence.data.object) return response(404);

  const downloaded = await downloadActivityEvidence(id, evidenceId, session.bearer);
  if (downloaded.issue || !downloaded.data) return response(downloaded.issue === "access" ? 403 : downloaded.issue === "not-found" ? 404 : 503);
  if (downloaded.data.contentType !== evidence.data.object.contentType.toLowerCase()) return response(503);
  const bytes = new Uint8Array(new ArrayBuffer(downloaded.data.bytes.byteLength));
  bytes.set(downloaded.data.bytes);

  return new Response(bytes.buffer, {
    status: 200,
    headers: {
      "Content-Type": downloaded.data.contentType,
      "Content-Length": String(bytes.byteLength),
      "Content-Disposition": disposition(evidence.data.object.filename),
      "Cache-Control": "private, no-store",
      "X-Content-Type-Options": "nosniff",
    },
  });
}
