import { useNavigate } from 'react-router-dom'

function PrivacyPage() {
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
          <h1 className="text-lg font-bold">개인정보 수집·이용 동의</h1>
        </div>

        <div className="bg-surface rounded-2xl p-6 text-sm leading-relaxed space-y-4">
          <section>
            <h2 className="font-bold mb-2">1. 수집하는 개인정보 항목</h2>
            <ul className="text-textsub list-disc pl-5 space-y-1">
              <li>필수: 이메일, 비밀번호, 닉네임, 생년월일, 휴대폰 번호</li>
              <li>자동 수집: 서비스 이용 기록(재실 상태 변경, 메시지, 식물 활동)</li>
            </ul>
          </section>

          <section>
            <h2 className="font-bold mb-2">2. 수집·이용 목적</h2>
            <ul className="text-textsub list-disc pl-5 space-y-1">
              <li>회원 식별 및 본인 인증</li>
              <li>서비스 제공(가족 그룹 기능, 알림, 통계)</li>
              <li>아이디/비밀번호 찾기 등 계정 복구</li>
              <li>이용 약관 위반 사항 확인</li>
            </ul>
          </section>

          <section>
            <h2 className="font-bold mb-2">3. 보유·이용 기간</h2>
            <p className="text-textsub">
              회원 탈퇴 시 즉시 파기합니다. 단, 관련 법령에 따라 보관이 필요한 정보는 해당 기간 동안 보관합니다.
            </p>
          </section>

          <section>
            <h2 className="font-bold mb-2">4. 동의 거부 권리</h2>
            <p className="text-textsub">
              필수 항목 수집·이용 동의를 거부할 수 있으나, 이 경우 회원가입 및 서비스 이용이 불가합니다.
            </p>
          </section>

          <p className="text-xs text-textsub pt-4 border-t border-borderlight">
            본 동의서는 베타 서비스 단계 임시 동의서이며, 정식 서비스 시작 시 갱신될 수 있습니다.
          </p>
        </div>
      </div>
    </div>
  )
}

export default PrivacyPage
