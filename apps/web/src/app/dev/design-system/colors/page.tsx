import { notFound } from "next/navigation";
import { ColorFoundationLab } from "../../../../components/color-foundation-lab";
import { FoundationLabNavigation } from "../../../../components/foundation-lab-navigation";

export default function DesignSystemColorsPage() {
  if (process.env.NODE_ENV === "production") notFound();
  return (
    <>
      <FoundationLabNavigation current="colors" />
      <ColorFoundationLab />
    </>
  );
}
