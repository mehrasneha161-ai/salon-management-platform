import { useState } from 'react'
import { Typography, Card, Input, Button, Row, Col, Alert, Space } from 'antd'
import { SendOutlined, NotificationOutlined } from '@ant-design/icons'
import {
  useSendBroadcastMutation,
  useSendCampaignMutation,
} from '../../features/notification/notificationApi'
import toast from 'react-hot-toast'

const { Title, Text } = Typography
const { TextArea } = Input

export default function AdminNotificationsPage() {
  const [sendBroadcast, { isLoading: broadcasting }] = useSendBroadcastMutation()
  const [sendCampaign, { isLoading: campaigning }] = useSendCampaignMutation()

  const [broadcastMsg, setBroadcastMsg] = useState('')
  const [phones, setPhones] = useState('')
  const [campaignMsg, setCampaignMsg] = useState('')

  const doBroadcast = async () => {
    const phoneNumbers = phones
      .split(/[,\s]+/)
      .map((p) => p.trim())
      .filter(Boolean)
    if (!broadcastMsg.trim()) { toast.error('Please enter a message'); return }
    if (phoneNumbers.length === 0) { toast.error('Please enter at least one phone number'); return }
    try {
      await sendBroadcast({ message: broadcastMsg, phoneNumbers }).unwrap()
      toast.success(`Broadcast queued for ${phoneNumbers.length} recipient(s)`)
      setBroadcastMsg('')
      setPhones('')
    } catch {
      toast.error('Could not queue broadcast')
    }
  }

  const doCampaign = async () => {
    if (!campaignMsg.trim()) { toast.error('Please enter a message'); return }
    try {
      await sendCampaign(campaignMsg).unwrap()
      toast.success('Campaign queued for all customers')
      setCampaignMsg('')
    } catch {
      toast.error('Could not queue campaign')
    }
  }

  return (
    <div>
      <div className="mb-6">
        <Title level={3}>Notifications</Title>
        <Text type="secondary">Send WhatsApp broadcasts to selected numbers, or a campaign to all customers.</Text>
      </div>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title={<Space><SendOutlined /> Broadcast (selected numbers)</Space>}>
            <Space direction="vertical" size="middle" style={{ width: '100%' }}>
              <TextArea
                rows={4}
                placeholder="Message to send…"
                value={broadcastMsg}
                onChange={(e) => setBroadcastMsg(e.target.value)}
              />
              <TextArea
                rows={3}
                placeholder="Phone numbers, comma or space separated (e.g. 9812300001, 9812300002)"
                value={phones}
                onChange={(e) => setPhones(e.target.value)}
              />
              <Button type="primary" loading={broadcasting} onClick={doBroadcast} block>
                Send broadcast
              </Button>
            </Space>
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card title={<Space><NotificationOutlined /> Marketing campaign (all customers)</Space>}>
            <Space direction="vertical" size="middle" style={{ width: '100%' }}>
              <TextArea
                rows={4}
                placeholder="Campaign message for every registered customer…"
                value={campaignMsg}
                onChange={(e) => setCampaignMsg(e.target.value)}
              />
              <Alert
                type="warning"
                showIcon
                message="This sends to ALL customers"
                description="The message is queued for every registered customer. Please double-check the text before sending."
              />
              <Button type="primary" danger loading={campaigning} onClick={doCampaign} block>
                Send campaign
              </Button>
            </Space>
          </Card>
        </Col>
      </Row>
    </div>
  )
}
