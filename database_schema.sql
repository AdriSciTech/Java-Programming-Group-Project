-- Finance Tracker Database Schema for Supabase
-- Run this in your Supabase SQL Editor

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users Table
CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    profile_picture_url TEXT,
    currency_preference VARCHAR(3) DEFAULT 'USD'
);

-- Categories Table
CREATE TABLE categories (
    category_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
    category_name VARCHAR(100) NOT NULL,
    category_type VARCHAR(20) CHECK (category_type IN ('INCOME', 'EXPENSE')),
    parent_category_id UUID REFERENCES categories(category_id) ON DELETE SET NULL,
    color_code VARCHAR(7),
    icon_name VARCHAR(50),
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Income Table
CREATE TABLE income (
    income_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
    category_id UUID REFERENCES categories(category_id) ON DELETE SET NULL,
    amount DECIMAL(15, 2) NOT NULL CHECK (amount > 0),
    source VARCHAR(255) NOT NULL,
    description TEXT,
    income_date DATE NOT NULL,
    is_recurring BOOLEAN DEFAULT FALSE,
    recurring_frequency VARCHAR(20),
    recurring_day INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Expenses Table
CREATE TABLE expenses (
    expense_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
    category_id UUID REFERENCES categories(category_id) ON DELETE SET NULL,
    amount DECIMAL(15, 2) NOT NULL CHECK (amount > 0),
    vendor VARCHAR(255),
    description TEXT,
    expense_date DATE NOT NULL,
    payment_method VARCHAR(50),
    receipt_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bills and Subscriptions Table
CREATE TABLE bills_subscriptions (
    bill_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
    category_id UUID REFERENCES categories(category_id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL CHECK (amount > 0),
    billing_cycle VARCHAR(20) CHECK (billing_cycle IN ('DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY')),
    due_day INTEGER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    is_active BOOLEAN DEFAULT TRUE,
    reminder_days INTEGER DEFAULT 3,
    last_payment_date DATE,
    next_payment_date DATE,
    description TEXT,
    vendor VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Budgets Table
CREATE TABLE budgets (
    budget_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
    category_id UUID REFERENCES categories(category_id) ON DELETE SET NULL,
    budget_name VARCHAR(255) NOT NULL,
    amount_limit DECIMAL(15, 2) NOT NULL CHECK (amount_limit > 0),
    period VARCHAR(20) CHECK (period IN ('WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY')),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    alert_threshold INTEGER DEFAULT 80 CHECK (alert_threshold BETWEEN 0 AND 100),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Accounts Table (for transfers)
CREATE TABLE accounts (
    account_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
    account_name VARCHAR(255) NOT NULL,
    account_type VARCHAR(50) CHECK (account_type IN ('CHECKING', 'SAVINGS', 'CREDIT_CARD', 'CASH', 'INVESTMENT', 'OTHER')),
    balance DECIMAL(15, 2) DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'USD',
    institution_name VARCHAR(255),
    account_number VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Transfers Table
CREATE TABLE transfers (
    transfer_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
    from_account_id UUID REFERENCES accounts(account_id) ON DELETE SET NULL,
    to_account_id UUID REFERENCES accounts(account_id) ON DELETE SET NULL,
    amount DECIMAL(15, 2) NOT NULL CHECK (amount > 0),
    transfer_date DATE NOT NULL,
    description TEXT,
    transfer_type VARCHAR(20) CHECK (transfer_type IN ('INTERNAL', 'EXTERNAL')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Investments Table
CREATE TABLE investments (
    investment_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
    account_id UUID REFERENCES accounts(account_id) ON DELETE SET NULL,
    investment_name VARCHAR(255) NOT NULL,
    investment_type VARCHAR(50) CHECK (investment_type IN ('STOCK', 'BOND', 'MUTUAL_FUND', 'ETF', 'CRYPTO', 'REAL_ESTATE', 'RETIREMENT', 'OTHER')),
    symbol VARCHAR(20),
    quantity DECIMAL(15, 6),
    purchase_price DECIMAL(15, 2),
    current_price DECIMAL(15, 2),
    purchase_date DATE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Budget Alerts Table
CREATE TABLE budget_alerts (
    alert_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
    budget_id UUID REFERENCES budgets(budget_id) ON DELETE CASCADE,
    alert_type VARCHAR(20) CHECK (alert_type IN ('WARNING', 'DANGER', 'EXCEEDED')),
    alert_message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_income_user_date ON income(user_id, income_date DESC);
CREATE INDEX idx_expenses_user_date ON expenses(user_id, expense_date DESC);
CREATE INDEX idx_expenses_category ON expenses(category_id);
CREATE INDEX idx_bills_user_active ON bills_subscriptions(user_id, is_active);
CREATE INDEX idx_budgets_user_active ON budgets(user_id, is_active);
CREATE INDEX idx_transfers_user_date ON transfers(user_id, transfer_date DESC);
CREATE INDEX idx_investments_user ON investments(user_id);
CREATE INDEX idx_alerts_user_unread ON budget_alerts(user_id, is_read);

-- Create updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply updated_at triggers
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_income_updated_at BEFORE UPDATE ON income
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_expenses_updated_at BEFORE UPDATE ON expenses
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_bills_updated_at BEFORE UPDATE ON bills_subscriptions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_budgets_updated_at BEFORE UPDATE ON budgets
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_accounts_updated_at BEFORE UPDATE ON accounts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_investments_updated_at BEFORE UPDATE ON investments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert default categories
INSERT INTO categories (category_name, category_type, is_default, color_code) VALUES
-- Income Categories
('Salary', 'INCOME', TRUE, '#4CAF50'),
('Freelance', 'INCOME', TRUE, '#8BC34A'),
('Business', 'INCOME', TRUE, '#CDDC39'),
('Investments', 'INCOME', TRUE, '#FFC107'),
('Bonus', 'INCOME', TRUE, '#FF9800'),
('Gift', 'INCOME', TRUE, '#FF5722'),
('Other Income', 'INCOME', TRUE, '#9E9E9E'),

-- Expense Categories
('Housing', 'EXPENSE', TRUE, '#F44336'),
('Food & Dining', 'EXPENSE', TRUE, '#E91E63'),
('Transportation', 'EXPENSE', TRUE, '#9C27B0'),
('Utilities', 'EXPENSE', TRUE, '#673AB7'),
('Healthcare', 'EXPENSE', TRUE, '#3F51B5'),
('Entertainment', 'EXPENSE', TRUE, '#2196F3'),
('Shopping', 'EXPENSE', TRUE, '#03A9F4'),
('Education', 'EXPENSE', TRUE, '#00BCD4'),
('Insurance', 'EXPENSE', TRUE, '#009688'),
('Personal Care', 'EXPENSE', TRUE, '#4CAF50'),
('Savings', 'EXPENSE', TRUE, '#8BC34A'),
('Debt Payment', 'EXPENSE', TRUE, '#CDDC39'),
('Other Expense', 'EXPENSE', TRUE, '#9E9E9E');

-- Enable Row Level Security (RLS)
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE income ENABLE ROW LEVEL SECURITY;
ALTER TABLE expenses ENABLE ROW LEVEL SECURITY;
ALTER TABLE bills_subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE budgets ENABLE ROW LEVEL SECURITY;
ALTER TABLE accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE transfers ENABLE ROW LEVEL SECURITY;
ALTER TABLE investments ENABLE ROW LEVEL SECURITY;
ALTER TABLE budget_alerts ENABLE ROW LEVEL SECURITY;

-- RLS Policies (Users can only access their own data)
CREATE POLICY user_policy ON users FOR ALL USING (auth.uid() = user_id);
CREATE POLICY category_policy ON categories FOR ALL USING (auth.uid() = user_id OR is_default = TRUE);
CREATE POLICY income_policy ON income FOR ALL USING (auth.uid() = user_id);
CREATE POLICY expense_policy ON expenses FOR ALL USING (auth.uid() = user_id);
CREATE POLICY bills_policy ON bills_subscriptions FOR ALL USING (auth.uid() = user_id);
CREATE POLICY budget_policy ON budgets FOR ALL USING (auth.uid() = user_id);
CREATE POLICY account_policy ON accounts FOR ALL USING (auth.uid() = user_id);
CREATE POLICY transfer_policy ON transfers FOR ALL USING (auth.uid() = user_id);
CREATE POLICY investment_policy ON investments FOR ALL USING (auth.uid() = user_id);
CREATE POLICY alert_policy ON budget_alerts FOR ALL USING (auth.uid() = user_id);
