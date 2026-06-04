-- =============================================
-- V7: Support Tickets
-- User tạo ticket báo lỗi/hỗ trợ
-- Admin/Staff xử lý
-- =============================================

CREATE TABLE auth.support_tickets (
                                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                      title VARCHAR(255) NOT NULL,
                                      description TEXT NOT NULL,
                                      status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
                                      priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
                                      created_by UUID NOT NULL REFERENCES auth.users(id),
                                      assigned_to UUID REFERENCES auth.users(id),
                                      created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                                      updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                                      closed_at TIMESTAMP WITH TIME ZONE
);

-- Index cho query thường dùng
CREATE INDEX idx_tickets_created_by ON auth.support_tickets(created_by);
CREATE INDEX idx_tickets_assigned_to ON auth.support_tickets(assigned_to);
CREATE INDEX idx_tickets_status ON auth.support_tickets(status);