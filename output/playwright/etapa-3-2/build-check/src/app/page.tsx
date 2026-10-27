import { DiaryWorkspace } from "@/components/diary-workspace";

export default function Home() {
  return <DiaryWorkspace accessToken={null} apiBaseUrl={process.env.NEXT_PUBLIC_NITE_API_URL ?? ""} />;
}
