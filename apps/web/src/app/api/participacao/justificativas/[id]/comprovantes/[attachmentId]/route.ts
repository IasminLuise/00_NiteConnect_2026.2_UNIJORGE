import { getAuthenticatedNiteSession } from "../../../../../../../lib/auth/server-session";
import { downloadJustificationAttachment } from "../../../../../../../lib/participation/mutations";
import { getJustificationAttachments } from "../../../../../../../lib/participation/data";

export const dynamic = "force-dynamic";
type RouteContext = { params: Promise<{ id: string; attachmentId: string }> };
const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function response(status: number): Response {
  return new Response(null, { status, headers: { "Cache-Control": "private, no-store", "X-Content-Type-Options": "nosniff" } });
}
function filename(value: string): string {
  return value.normalize("NFKC").replace(/[\\/\u0000-\u001f\u007f]/g, "_").replace(/^\.+/, "").slice(0, 160) || "comprovante";
}
function disposition(value: string): string {
  const safe = filename(value);
  const fallback = safe.replace(/[^\x20-\x7e]|["\\;]/g, "_").replace(/^[. ]+|[. ]+$/g, "") || "comprovante";
  const encoded = encodeURIComponent(safe).replace(/[!'()*]/g, (character) => `%${character.charCodeAt(0).toString(16).toUpperCase()}`);
  return `attachment; filename="${fallback}"; filename*=UTF-8''${encoded}`;
}

export async function GET(_request: Request, { params }: RouteContext): Promise<Response> {
  const { id, attachmentId } = await params;
  if (!uuidPattern.test(id) || !uuidPattern.test(attachmentId)) return response(404);
  const session = await getAuthenticatedNiteSession();
  if (!session) return response(401);
  const collection = await getJustificationAttachments(session.bearer, id);
  if (collection.issue) return response(collection.issue === "access" ? 403 : collection.issue === "not-found" ? 404 : 503);
  const attachment = collection.data.items.find((item) => item.id === attachmentId);
  if (!attachment?.downloadable) return response(404);
  const downloaded = await downloadJustificationAttachment(id, attachmentId, session.bearer);
  if (downloaded.issue || !downloaded.data) return response(downloaded.issue === "access" ? 403 : downloaded.issue === "not-found" ? 404 : 503);
  if (downloaded.data.contentType !== attachment.contentType.toLowerCase() || downloaded.data.bytes.byteLength > 5_000_000) return response(503);
  const bytes = new Uint8Array(downloaded.data.bytes);
  return new Response(bytes.buffer, { status: 200, headers: {
    "Content-Type": downloaded.data.contentType,
    "Content-Length": String(bytes.byteLength),
    "Content-Disposition": disposition(attachment.filename),
    "Cache-Control": "private, no-store",
    "X-Content-Type-Options": "nosniff",
  } });
}
