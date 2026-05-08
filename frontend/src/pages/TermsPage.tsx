import { useNavigate } from 'react-router-dom'

function TermsPage() {
  const navigate = useNavigate()

  return (
    <div className="min-h-screen px-6 py-12">
      <div className="max-w-2xl mx-auto">
        <div className="flex items-center mb-6">
          <button
            onClick={() => navigate(-1)}
            className="text-2xl text-darkbrown mr-3"
            aria-label="뒤로"
          >
            ←
          </button>
          <h1 className="text-lg font-bold">이용약관</h1>
        </div>

        <div className="bg-surface rounded-2xl p-6 text-sm leading-relaxed space-y-4">
          <section>
            <h2 className="font-bold mb-2">제1조 (목적)</h2>
            <p className="text-textsub">
              본 약관은 우리집 서비스(이하 "서비스")의 이용 조건과 절차, 회원과 회사의 권리·의무 및 책임 사항을 규정함을 목적으로 합니다.
            </p>
          </section>

          <section>
            <h2 className="font-bold mb-2">제2조 (회원 가입)</h2>
            <p className="text-textsub">
              만 14세 이상인 자가 본 약관에 동의하고 회원가입 절차를 완료한 후, 회사가 이를 승낙함으로써 회원 자격을 취득합니다.
            </p>
          </section>

          <section>
            <h2 className="font-bold mb-2">제3조 (서비스 이용)</h2>
            <p className="text-textsub">
              회원은 가족 그룹 내에서 재실 상태 공유, 캘린더, 메시지, 공용 식물 등의 기능을 이용할 수 있습니다.
            </p>
          </section>

          <section>
            <h2 className="font-bold mb-2">제4조 (계정 관리)</h2>
            <p className="text-textsub">
              회원은 본인의 이메일과 비밀번호를 안전하게 관리할 책임이 있으며, 제3자에게 양도하거나 대여할 수 없습니다.
            </p>
          </section>

          <section>
            <h2 className="font-bold mb-2">제5조 (서비스 변경 및 중단)</h2>
            <p className="text-textsub">
              회사는 운영상·기술상 필요에 따라 서비스의 일부 또는 전부를 변경하거나 중단할 수 있으며, 사전에 회원에게 공지합니다.
            </p>
          </section>

          <p className="text-xs text-textsub pt-4 border-t border-borderlight">
            본 약관은 베타 서비스 단계 임시 약관이며, 정식 서비스 시작 시 갱신될 수 있습니다.
          </p>
        </div>
      </div>
    </div>
  )
}

export default TermsPage
