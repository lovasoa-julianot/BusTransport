export default function SubmitButton({ label }) {
  return (
    <button
      type="submit"
      className="w-full bg-[#4d7c65] hover:bg-[#3d6b54] text-white p-2.5 rounded-lg text-sm font-bold uppercase tracking-wider"
    >
      {label}
    </button>
  );
}
